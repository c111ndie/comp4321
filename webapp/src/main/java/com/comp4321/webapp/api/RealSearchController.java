package com.comp4321.webapp.api;

import com.comp4321.search.Query;
import com.comp4321.search.Search;
import com.comp4321.spider.indexer.PageMeta;
import com.comp4321.spider.indexer.StopStem;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.helper.FastIterator;
import jdbm.htree.HTree;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Real search controller backed by the JDBM index built by the spider.
 * Activated when webapp.mock-search.enabled=false (or absent).
 */
@RestController
@ConditionalOnProperty(name = "webapp.mock-search.enabled", havingValue = "false", matchIfMissing = false)
public class RealSearchController {

    @Value("${search.db-name:indexDB}")
    private String dbName;

    @Value("${search.stopwords:stopwords.txt}")
    private String stopwordsPath;

    private RecordManager recman;
    private HTree wordToWordId;
    private HTree bodyInvertedIndex;
    private HTree titleInvertedIndex;
    private HTree pageMetadata;
    private StopStem stopStem;

    @PostConstruct
    public void init() throws IOException {
        recman = RecordManagerFactory.createRecordManager(dbName);

        Long wordIdRec = recman.getNamedObject("wordToWordId");
        Long bodyIdRec = recman.getNamedObject("bodyInvertedIndex");
        Long metaIdRec = recman.getNamedObject("pageMetadata");
        Long titleIdRec = recman.getNamedObject("titleInvertedIndex");

        if (wordIdRec == null || bodyIdRec == null || metaIdRec == null || titleIdRec == null) {
            throw new IllegalStateException(
                    "JDBM named objects not found in '" + dbName + "'. " +
                            "Please run the spider first to build the index.");
        }

        wordToWordId = HTree.load(recman, wordIdRec);
        bodyInvertedIndex = HTree.load(recman, bodyIdRec);
        titleInvertedIndex = HTree.load(recman, titleIdRec);
        pageMetadata = HTree.load(recman, metaIdRec);

        stopStem = new StopStem(stopwordsPath);
        System.out.println("[RealSearchController] JDBM index loaded from: " + dbName);
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (recman != null)
                recman.close();
        } catch (IOException e) {
            System.err.println("[RealSearchController] Error closing JDBM: " + e.getMessage());
        }
    }

    @GetMapping("/api/search")
    public MockSearchController.SearchResponse search(
            @RequestParam(name = "q", defaultValue = "") String queryStr,
            @RequestParam(name = "max", defaultValue = "50") int max) throws IOException {

        if (queryStr == null || queryStr.isBlank()) {
            return new MockSearchController.SearchResponse(queryStr, 0, Collections.emptyList());
        }

        int limit = Math.max(1, Math.min(max, 50));
        Query query = parseQuery(queryStr);

        if (query.getSingleTerms().isEmpty() && query.getPhrases().isEmpty()) {
            return new MockSearchController.SearchResponse(queryStr, 0, Collections.emptyList());
        }

        Search search = new Search(query, wordToWordId, bodyInvertedIndex, titleInvertedIndex, pageMetadata);
        List<Search.SearchResult> results = search.execute();

        List<MockSearchController.SearchResultItem> items = new ArrayList<>();

        // Collect all query stems (single terms + phrase words) for missing-term
        // detection
        List<String> allQueryStems = new ArrayList<>(query.getSingleTerms());
        for (List<String> phrase : query.getPhrases()) {
            allQueryStems.addAll(phrase);
        }
        // Remove duplicates while preserving order
        List<String> uniqueQueryStems = allQueryStems.stream().distinct().collect(Collectors.toList());

        // For each query stem, pre-fetch the set of page IDs that contain it (body or
        // title)
        // Key: stem, Value: set of docIds containing that stem
        Map<String, Set<Integer>> stemPageIds = new java.util.HashMap<>();
        if (uniqueQueryStems.size() > 1) {
            for (String stem : uniqueQueryStems) {
                Integer wordId = (Integer) wordToWordId.get(stem);
                Set<Integer> pages = new java.util.HashSet<>();
                if (wordId != null) {
                    com.comp4321.spider.indexer.PostingList bodyPl = (com.comp4321.spider.indexer.PostingList) bodyInvertedIndex
                            .get(wordId);
                    if (bodyPl != null)
                        pages.addAll(bodyPl.getPageIds());
                    com.comp4321.spider.indexer.PostingList titlePl = (com.comp4321.spider.indexer.PostingList) titleInvertedIndex
                            .get(wordId);
                    if (titlePl != null)
                        pages.addAll(titlePl.getPageIds());
                }
                stemPageIds.put(stem, pages);
            }
        }

        for (Search.SearchResult r : results.stream().limit(limit).collect(Collectors.toList())) {
            PageMeta meta = (PageMeta) pageMetadata.get(r.docId);
            if (meta == null)
                continue;
            MockSearchController.SearchResultItem item = toResultItem(meta, r.score);

            // Compute which query stems are absent from this page (body or title)
            if (uniqueQueryStems.size() > 1) {
                List<String> missing = uniqueQueryStems.stream()
                        .filter(stem -> !stemPageIds.getOrDefault(stem, java.util.Collections.emptySet())
                                .contains(r.docId))
                        .collect(Collectors.toList());
                if (!missing.isEmpty()) {
                    item.setMissingTerms(missing);
                }
            }

            items.add(item);
        }

        return new MockSearchController.SearchResponse(queryStr, items.size(), items);
    }

    @GetMapping("/api/suggest")
    public List<String> suggest(@RequestParam(name = "q", defaultValue = "") String queryStr) throws IOException {
        String needle = queryStr == null ? "" : queryStr.toLowerCase().trim();
        Set<String> suggestions = new LinkedHashSet<>();

        FastIterator it = wordToWordId.keys();
        Object key;
        while ((key = it.next()) != null && suggestions.size() < 40) {
            String stem = (String) key;
            if (needle.isEmpty() || stem.contains(needle)) {
                suggestions.add(stem);
            }
        }
        return new ArrayList<>(suggestions);
    }

    @GetMapping("/api/keywords")
    public List<String> keywords() throws IOException {
        List<String> all = new ArrayList<>();
        FastIterator it = wordToWordId.keys();
        Object key;
        while ((key = it.next()) != null) {
            all.add((String) key);
        }
        Collections.sort(all);
        return all;
    }

    // ---- Query parsing: tokenise, remove stop words, stem ----

    private Query parseQuery(String rawQuery) {
        List<String> singleTerms = new ArrayList<>();
        List<List<String>> phrases = new ArrayList<>();

        List<String> tokens = tokenizePreserveQuotes(rawQuery);
        for (String token : tokens) {
            if (token.startsWith("\"") && token.endsWith("\"") && token.length() > 1) {
                String phraseContent = token.substring(1, token.length() - 1);
                String[] words = phraseContent.split("\\s+");
                List<String> stemmedPhrase = new ArrayList<>();
                for (String w : words) {
                    String lower = w.toLowerCase();
                    if (!stopStem.isStopWord(lower)) {
                        String stem = stopStem.stem(lower);
                        if (!stem.isEmpty())
                            stemmedPhrase.add(stem);
                    }
                }
                if (!stemmedPhrase.isEmpty())
                    phrases.add(stemmedPhrase);
            } else {
                // Skip excluded terms (prefixed with -)
                if (token.startsWith("-"))
                    continue;
                String lower = token.toLowerCase();
                if (!stopStem.isStopWord(lower)) {
                    String stem = stopStem.stem(lower);
                    if (!stem.isEmpty())
                        singleTerms.add(stem);
                }
            }
        }
        return new Query(singleTerms, phrases);
    }

    private List<String> tokenizePreserveQuotes(String text) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c == '"') {
                if (inQuotes) {
                    current.append(c);
                    result.add(current.toString());
                    current.setLength(0);
                    inQuotes = false;
                } else {
                    if (current.length() > 0) {
                        for (String part : current.toString().trim().split("\\s+")) {
                            if (!part.isEmpty())
                                result.add(part);
                        }
                        current.setLength(0);
                    }
                    current.append(c);
                    inQuotes = true;
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            if (inQuotes) {
                // unclosed quote — treat as plain terms
                String inner = current.toString().replaceFirst("^\"", "");
                for (String part : inner.trim().split("\\s+")) {
                    if (!part.isEmpty())
                        result.add(part);
                }
            } else {
                for (String part : current.toString().trim().split("\\s+")) {
                    if (!part.isEmpty())
                        result.add(part);
                }
            }
        }
        return result;
    }

    // ---- Build response item from PageMeta ----

    private MockSearchController.SearchResultItem toResultItem(PageMeta meta, double score) {
        List<MockSearchController.KeywordFreq> keywords = new ArrayList<>();
        int count = 0;
        for (Map.Entry<String, Integer> e : meta.topBodyStems.entrySet()) {
            if (count >= 5)
                break;
            keywords.add(new MockSearchController.KeywordFreq(e.getKey(), e.getValue()));
            count++;
        }

        return new MockSearchController.SearchResultItem(
                score,
                meta.title.isEmpty() ? meta.url : meta.title,
                meta.url,
                meta.lastModifiedRfc1123,
                meta.sizeBytes,
                keywords,
                meta.parentUrls,
                meta.childUrls);
    }
}

package com.comp4321.spider.indexer;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.helper.FastIterator;
import jdbm.htree.HTree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages all JDBM-backed data structures needed for Phase 1.
 *
 * <h3>JDBM Database Schema</h3>
 * 
 * <pre>
 * RecordManager "indexDB"  (or any name passed to constructor)
 * ├─ HTree "urlToPageId"       String(url)     → Integer(pageId)
 * ├─ HTree "pageIdToUrl"       Integer(pageId) → String(url)
 * ├─ HTree "wordToWordId"      String(stem)    → Integer(wordId)
 * ├─ HTree "wordIdToWord"      Integer(wordId) → String(stem)
 * ├─ HTree "bodyInvertedIndex" Integer(wordId) → PostingList
 * ├─ HTree "titleInvertedIndex"Integer(wordId) → PostingList
 * ├─ HTree "forwardIndex"      Integer(pageId) → HashMap&lt;Integer,Integer&gt; (wordId→freq)
 * ├─ HTree "pageMetadata"      Integer(pageId) → PageMeta
 * └─ HTree "counters"          String          → Integer  ("nextWordId")
 * </pre>
 *
 * Page IDs are taken directly from the spider's PageRecord.pageId so that
 * the two systems stay in sync without a second ID generator.
 */
public class JdbmIndexer {

    private final RecordManager recman;
    private final HTree urlToPageId;
    private final HTree pageIdToUrl;
    private final HTree wordToWordId;
    private final HTree wordIdToWord;
    private final HTree bodyInvertedIndex;
    private final HTree titleInvertedIndex;
    private final HTree forwardIndex;
    private final HTree pageMetadata;
    private final HTree counters;
    private final StopStem stopStem;

    public JdbmIndexer(String dbName, String stopwordsPath) throws IOException {
        recman = RecordManagerFactory.createRecordManager(dbName);
        urlToPageId = loadOrCreate("urlToPageId");
        pageIdToUrl = loadOrCreate("pageIdToUrl");
        wordToWordId = loadOrCreate("wordToWordId");
        wordIdToWord = loadOrCreate("wordIdToWord");
        bodyInvertedIndex = loadOrCreate("bodyInvertedIndex");
        titleInvertedIndex = loadOrCreate("titleInvertedIndex");
        forwardIndex = loadOrCreate("forwardIndex");
        pageMetadata = loadOrCreate("pageMetadata");
        counters = loadOrCreate("counters");
        stopStem = new StopStem(stopwordsPath);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Indexes a single page.
     *
     * @param pageId       Spider-assigned page ID (from PageRecord.pageId)
     * @param url          Canonical URL of the page
     * @param title        Page title (plain text)
     * @param bodyText     Visible body text extracted from HTML
     * @param lastModified RFC-1123 last-modified string
     * @param sizeBytes    Content length in bytes
     * @param childUrls    Out-link URLs (up to 10 stored in metadata)
     * @param parentUrls   Parent page URLs (pages linking to this page)
     */
    public void indexPage(int pageId, String url, String title, String bodyText,
            String lastModified, long sizeBytes,
            List<String> childUrls, List<String> parentUrls) throws IOException {

        // Register URL ↔ pageId mapping
        urlToPageId.put(url, pageId);
        pageIdToUrl.put(pageId, url);

        // Tokenise and stem title and body
        Map<Integer, List<Integer>> titlePositions = tokeniseAndStem(title);
        Map<Integer, List<Integer>> bodyPositions = tokeniseAndStem(bodyText);

        // Update title inverted index
        for (Map.Entry<Integer, List<Integer>> e : titlePositions.entrySet()) {
            int wordId = e.getKey();
            PostingList plist = (PostingList) titleInvertedIndex.get(wordId);
            if (plist == null)
                plist = new PostingList();
            for (int pos : e.getValue())
                plist.addOccurrence(pageId, pos);
            titleInvertedIndex.put(wordId, plist);
        }

        // Update body inverted index
        for (Map.Entry<Integer, List<Integer>> e : bodyPositions.entrySet()) {
            int wordId = e.getKey();
            PostingList plist = (PostingList) bodyInvertedIndex.get(wordId);
            if (plist == null)
                plist = new PostingList();
            for (int pos : e.getValue())
                plist.addOccurrence(pageId, pos);
            bodyInvertedIndex.put(wordId, plist);
        }

        // Forward index: pageId → {wordId → freq} (body only, for tf*idf in Phase 2)
        HashMap<Integer, Integer> bodyFreqMap = new HashMap<>();
        for (Map.Entry<Integer, List<Integer>> e : bodyPositions.entrySet()) {
            bodyFreqMap.put(e.getKey(), e.getValue().size());
        }
        forwardIndex.put(pageId, bodyFreqMap);

        // Build stem→freq map for metadata (readable stems, not word IDs)
        Map<String, Integer> stemFreqs = new HashMap<>();
        for (Map.Entry<Integer, List<Integer>> e : bodyPositions.entrySet()) {
            String stem = (String) wordIdToWord.get(e.getKey());
            if (stem != null)
                stemFreqs.merge(stem, e.getValue().size(), Integer::sum);
        }
        // Top 10 body stems by frequency
        Map<String, Integer> topStems = new LinkedHashMap<>();
        stemFreqs.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(10)
                .forEach(e -> topStems.put(e.getKey(), e.getValue()));

        // Build and store PageMeta
        PageMeta meta = new PageMeta();
        meta.pageId = pageId;
        meta.url = url;
        meta.title = title != null ? title : "";
        meta.lastModifiedRfc1123 = lastModified != null ? lastModified : "";
        meta.sizeBytes = sizeBytes;
        meta.childUrls = childUrls != null ? childUrls : new ArrayList<>();
        meta.parentUrls = parentUrls != null ? parentUrls : new ArrayList<>();
        meta.topBodyStems = topStems;
        pageMetadata.put(pageId, meta);

        recman.commit();
    }

    /** Returns the PageMeta for the given pageId, or null if not found. */
    public PageMeta getPageMeta(int pageId) throws IOException {
        return (PageMeta) pageMetadata.get(pageId);
    }

    /** Returns all indexed PageMeta objects sorted by pageId ascending. */
    public List<PageMeta> getAllPageMetas() throws IOException {
        List<PageMeta> result = new ArrayList<>();
        FastIterator it = pageMetadata.keys();
        Object key;
        while ((key = it.next()) != null) {
            PageMeta meta = (PageMeta) pageMetadata.get(key);
            if (meta != null)
                result.add(meta);
        }
        result.sort(Comparator.comparingInt(m -> m.pageId));
        return result;
    }

    /** Returns the posting list for a body term (by stem string), or null. */
    public PostingList getBodyPostingList(String stem) throws IOException {
        Integer wordId = (Integer) wordToWordId.get(stem);
        if (wordId == null)
            return null;
        return (PostingList) bodyInvertedIndex.get(wordId);
    }

    /** Returns the posting list for a title term (by stem string), or null. */
    public PostingList getTitlePostingList(String stem) throws IOException {
        Integer wordId = (Integer) wordToWordId.get(stem);
        if (wordId == null)
            return null;
        return (PostingList) titleInvertedIndex.get(wordId);
    }

    /** Forward index entry: pageId → {wordId → frequency in body}. */
    @SuppressWarnings("unchecked")
    public Map<Integer, Integer> getForwardIndex(int pageId) throws IOException {
        return (Map<Integer, Integer>) forwardIndex.get(pageId);
    }

    public void commit() throws IOException {
        recman.commit();
    }

    public void close() throws IOException {
        recman.commit();
        recman.close();
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private HTree loadOrCreate(String name) throws IOException {
        long recid = recman.getNamedObject(name);
        if (recid != 0)
            return HTree.load(recman, recid);
        HTree tree = HTree.createInstance(recman);
        recman.setNamedObject(name, tree.getRecid());
        return tree;
    }

    /**
     * Tokenises the text, removes stop words, stems each token, and maps each
     * stem to its list of token positions (0-based). New stems are assigned
     * a fresh wordId and registered in wordToWordId / wordIdToWord.
     */
    private Map<Integer, List<Integer>> tokeniseAndStem(String text) throws IOException {
        Map<Integer, List<Integer>> wordPositions = new LinkedHashMap<>();
        if (text == null || text.isBlank())
            return wordPositions;

        String[] tokens = text.split("\\W+");
        int pos = 0;
        for (String token : tokens) {
            if (token.isEmpty()) {
                pos++;
                continue;
            }
            String lower = token.toLowerCase();
            if (!stopStem.isStopWord(lower)) {
                String stem = stopStem.stem(lower);
                if (!stem.isEmpty()) {
                    int wordId = getOrCreateWordId(stem);
                    wordPositions.computeIfAbsent(wordId, k -> new ArrayList<>()).add(pos);
                }
            }
            pos++;
        }
        return wordPositions;
    }

    private int getOrCreateWordId(String stem) throws IOException {
        Integer existing = (Integer) wordToWordId.get(stem);
        if (existing != null)
            return existing;

        Integer next = (Integer) counters.get("nextWordId");
        int id = (next == null) ? 0 : next;

        wordToWordId.put(stem, id);
        wordIdToWord.put(id, stem);
        counters.put("nextWordId", id + 1);
        return id;
    }
}

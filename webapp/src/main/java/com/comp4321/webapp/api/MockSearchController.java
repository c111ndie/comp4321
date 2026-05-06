package com.comp4321.webapp.api;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@ConditionalOnProperty(name = "webapp.mock-search.enabled", havingValue = "true", matchIfMissing = true)
public class MockSearchController {
    private static final Pattern PHRASE_PATTERN = Pattern.compile("\"([^\"]+)\"");
    private static final List<MockPage> PAGES = buildPages();
    private static final List<String> KEYWORD_CATALOG = buildKeywordCatalog();

    @GetMapping("/api/search")
    public SearchResponse search(@RequestParam(name = "q", defaultValue = "") String query,
                                 @RequestParam(name = "max", defaultValue = "50") int max) {
        QueryParts parts = parseQuery(query);
        if (parts.isEmpty()) {
            return new SearchResponse(query, 0, Collections.emptyList());
        }

        int limit = Math.max(1, Math.min(max, 50));
        List<SearchResultItem> matches = PAGES.stream()
            .map(page -> toSearchResult(page, parts))
            .filter(item -> item != null)
            .sorted(Comparator.comparingDouble(SearchResultItem::getScore).reversed()
                .thenComparing(SearchResultItem::getTitle))
            .collect(Collectors.toList());

        List<SearchResultItem> combined = new ArrayList<>(matches);
        combined.addAll(generateSyntheticResults(query, parts, limit - combined.size(), combined));
        combined = combined.stream().limit(limit).collect(Collectors.toList());

        return new SearchResponse(query, combined.size(), combined);
    }

    @GetMapping("/api/suggest")
    public List<String> suggest(@RequestParam(name = "q", defaultValue = "") String query) {
        String needle = normalize(query);
        if (needle.isEmpty()) {
            return KEYWORD_CATALOG.stream().limit(40).collect(Collectors.toList());
        }

        Set<String> suggestions = new LinkedHashSet<>();
        for (String keyword : KEYWORD_CATALOG) {
            if (suggestions.size() >= 8) {
                break;
            }
            if (normalize(keyword).contains(needle)) {
                suggestions.add(keyword);
            }
        }
        for (String variant : generateKeywordVariants(query)) {
            if (suggestions.size() >= 40) {
                break;
            }
            suggestions.add(variant);
        }
        return suggestions.stream().limit(40).collect(Collectors.toList());
    }

    @GetMapping("/api/keywords")
    public List<String> keywords() {
        return KEYWORD_CATALOG;
    }

    private SearchResultItem toSearchResult(MockPage page, QueryParts parts) {
        String title = normalize(page.title);
        String url = normalize(page.url);
        String keywords = page.keywords.stream()
            .map(keyword -> normalize(keyword.term))
            .collect(Collectors.joining(" "));
        String combined = title + " " + url + " " + keywords;

        for (String excludedTerm : parts.excludedTerms) {
            String normalizedExcluded = normalize(excludedTerm);
            if (!normalizedExcluded.isEmpty() && combined.contains(normalizedExcluded)) {
                return null;
            }
        }

        double score = 0.0;

        for (String phrase : parts.phrases) {
            String normalizedPhrase = normalize(phrase);
            if (!combined.contains(normalizedPhrase)) {
                return null;
            }
            score += title.contains(normalizedPhrase) ? 0.60 : 0.35;
        }

        boolean matchedAnyTerm = parts.terms.isEmpty();
        for (String term : parts.terms) {
            String normalizedTerm = normalize(term);
            if (normalizedTerm.isEmpty()) {
                continue;
            }
            if (title.contains(normalizedTerm)) {
                score += 0.30;
                matchedAnyTerm = true;
            } else if (keywords.contains(normalizedTerm)) {
                score += 0.18;
                matchedAnyTerm = true;
            } else if (url.contains(normalizedTerm)) {
                score += 0.10;
                matchedAnyTerm = true;
            }
        }

        if (!matchedAnyTerm) {
            return null;
        }

        double cappedScore = Math.min(score, 0.99);
        return new SearchResultItem(
            cappedScore,
            page.title,
            page.url,
            page.lastModified,
            page.sizeBytes,
            page.keywords,
            page.parentLinks,
            page.childLinks
        );
    }

    private QueryParts parseQuery(String query) {
        Matcher matcher = PHRASE_PATTERN.matcher(query == null ? "" : query);
        List<String> phrases = new ArrayList<>();
        while (matcher.find()) {
            String phrase = matcher.group(1).trim();
            if (!phrase.isEmpty()) {
                phrases.add(phrase);
            }
        }

        String withoutPhrases = (query == null ? "" : query).replaceAll("\"[^\"]+\"", " ");
        List<String> excludedTerms = new ArrayList<>();
        List<String> terms = Arrays.stream(withoutPhrases.trim().split("\\s+"))
            .filter(token -> !token.isBlank())
            .filter(token -> {
                if (token.startsWith("-") && token.length() > 1) {
                    excludedTerms.add(token.substring(1));
                    return false;
                }
                return true;
            })
            .collect(Collectors.toList());

        return new QueryParts(phrases, terms, excludedTerms);
    }

    private List<SearchResultItem> generateSyntheticResults(String query, QueryParts parts, int needed,
                                                            List<SearchResultItem> existing) {
        if (needed <= 0) {
            return Collections.emptyList();
        }

        Set<String> usedTitles = existing.stream()
            .map(SearchResultItem::getTitle)
            .collect(Collectors.toCollection(HashSet::new));

        List<String> coreTerms = new ArrayList<>();
        coreTerms.addAll(parts.phrases);
        coreTerms.addAll(parts.terms);
        if (coreTerms.isEmpty()) {
            coreTerms.add(removeExcludedTerms(query));
        }

        String positiveQuery = removeExcludedTerms(query).replace("\"", "").trim();
        String displayQuery = toDisplayQuery(positiveQuery);
        String slug = slugify(positiveQuery);
        List<SearchResultItem> generated = new ArrayList<>();

        String[] titleTemplates = {
            "%s Resource Hub",
            "HKUST %s Guide",
            "%s Project Archive",
            "%s Research Overview",
            "%s Student Information",
            "%s Reference Page"
        };

        String[] pathTemplates = {
            "resources/%s",
            "guide/%s",
            "archive/%s",
            "research/%s",
            "student-life/%s",
            "reference/%s"
        };

        for (int i = 0; i < needed; i++) {
            String title = String.format(titleTemplates[i % titleTemplates.length], displayQuery);
            if (!usedTitles.add(title)) {
                title = title + " " + (i + 1);
            }

            String path = String.format(pathTemplates[i % pathTemplates.length], slug);
            String url = "https://www.cse.ust.hk/" + path + ".html";
            long sizeBytes = 6400L + (i * 1700L) + (slug.length() * 23L);
            double score = Math.max(0.34, 0.78 - (i * 0.07));

            generated.add(new SearchResultItem(
                score,
                title,
                url,
                "Tue, 27 Apr 2026 10:" + String.format("%02d", 10 + i) + ":00 GMT",
                sizeBytes,
                generateKeywords(coreTerms, i),
                generateParentLinks(slug),
                generateChildLinks(slug, i)
            ));
        }

        return generated;
    }

    private List<KeywordFreq> generateKeywords(List<String> coreTerms, int offset) {
        List<KeywordFreq> keywords = new ArrayList<>();
        int freq = 15 - offset;
        for (String term : coreTerms) {
            String clean = term.replace("\"", "").trim();
            if (!clean.isEmpty()) {
                keywords.add(keyword(clean.toLowerCase(Locale.ROOT), Math.max(4, freq)));
                freq -= 2;
            }
            if (keywords.size() == 3) {
                break;
            }
        }
        keywords.add(keyword("hkust", Math.max(4, 8 - offset)));
        keywords.add(keyword("cse", Math.max(4, 7 - offset)));
        while (keywords.size() > 5) {
            keywords.remove(keywords.size() - 1);
        }
        return keywords;
    }

    private List<String> generateParentLinks(String slug) {
        return links(
            "https://www.cse.ust.hk/",
            "https://www.cse.ust.hk/resources/",
            "https://www.cse.ust.hk/topics/" + slug + "/"
        );
    }

    private List<String> generateChildLinks(String slug, int index) {
        return links(
            "https://www.cse.ust.hk/" + slug + "/overview.html",
            "https://www.cse.ust.hk/" + slug + "/details-" + (index + 1) + ".html",
            "https://www.cse.ust.hk/" + slug + "/examples.html",
            "https://www.cse.ust.hk/" + slug + "/downloads/" + slug + ".pdf"
        );
    }

    private String slugify(String value) {
        String slug = normalize(value).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return slug.isBlank() ? "search-topic" : slug;
    }

    private String toDisplayQuery(String value) {
        String cleaned = value == null ? "" : value.replace("\"", "").trim();
        if (cleaned.isEmpty()) {
            return "Search Topic";
        }
        return Arrays.stream(cleaned.split("\\s+"))
            .map(word -> word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase(Locale.ROOT))
            .collect(Collectors.joining(" "));
    }

    private String removeExcludedTerms(String value) {
        if (value == null) {
            return "";
        }
        return Arrays.stream(value.trim().split("\\s+"))
            .filter(token -> !(token.startsWith("-") && token.length() > 1))
            .collect(Collectors.joining(" "));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static List<String> buildKeywordCatalog() {
        Set<String> catalog = new LinkedHashSet<>();
        for (MockPage page : PAGES) {
            for (KeywordFreq keyword : page.keywords) {
                catalog.add(keyword.term);
            }
        }

        catalog.addAll(Arrays.asList(
            "algorithm", "analysi", "artifici", "big data", "blockchain", "cloud",
            "cluster", "comput", "compress", "crawler", "cybersecur", "data",
            "databas", "dataset", "deep learn", "design", "engin", "evalu",
            "fyp", "graph", "hkust", "index", "inform", "intern", "keyword",
            "learn", "machin", "model", "network", "optim", "page", "poster",
            "predict", "process", "project", "query", "rank", "recommend",
            "report", "research", "retriev", "robot", "search", "secur",
            "stem", "student", "system", "technolog", "visual"
        ));

        return new ArrayList<>(catalog);
    }

    private List<String> generateKeywordVariants(String query) {
        String cleaned = normalize(query).trim();
        if (cleaned.isEmpty()) {
            return Collections.emptyList();
        }

        String[] suffixes = {
            "", " system", " model", " analysi", " project", " design",
            " research", " dataset", " method", " applic", " optim"
        };

        Set<String> variants = new LinkedHashSet<>();
        for (String suffix : suffixes) {
            variants.add(cleaned + suffix);
        }
        variants.add(cleaned.replace(" ", ""));
        variants.add(cleaned + " keyword");
        variants.add(cleaned + " search");
        return new ArrayList<>(variants);
    }

    private static List<MockPage> buildPages() {
        List<MockPage> pages = new ArrayList<>();

        pages.add(new MockPage(
            "FYP Posters",
            "https://www.cse.ust.hk/ct/fyp/poster.html",
            "Thu, 29 Apr 2021 00:00:00 GMT",
            17524L,
            keywords(
                keyword("poster", 19),
                keyword("fyp", 13),
                keyword("hkust", 8),
                keyword("cse", 7),
                keyword("report", 7)
            ),
            links(
                "https://www.cse.ust.hk/ct/",
                "https://www.cse.ust.hk/ug/fyp/?fyp_year=2016-2017",
                "https://www.cse.ust.hk/ug/fyp/?fyp_year=2018-2019",
                "https://www.cse.ust.hk/ct/fyp/poster.html",
                "https://www.cse.ust.hk/ug/fyp/?fyp_year=2017-2018"
            ),
            links(
                "https://www.cse.ust.hk/ct/fyp/",
                "https://www.cse.ust.hk/ct/fyp/reports/content/ieee_style.html",
                "https://www.cse.ust.hk/ct/fyp/reports/content/abstract.html",
                "http://www.cse.ust.hk/ug/fyp/bestfyp/2013-2014/QIAN1_Poster.pdf",
                "https://www.cse.ust.hk/ct/fyp/reports/content/fyp_report_quick_overview.pdf"
            )
        ));

        pages.add(new MockPage(
            "HKUST CSE CT Website",
            "https://www.cse.ust.hk/ct/",
            "Thu, 29 Apr 2021 00:00:00 GMT",
            16086L,
            keywords(
                keyword("ct", 12),
                keyword("fyp", 10),
                keyword("hkust", 7),
                keyword("website", 6),
                keyword("help", 5)
            ),
            links(
                "https://www.cse.ust.hk/",
                "https://www.ust.hk/"
            ),
            links(
                "https://www.cse.ust.hk/ct/fyp/",
                "https://www.cse.ust.hk/ct/fyp/reports/content/ieee_style.html",
                "https://www.cse.ust.hk/ct/fyp/reports/content/fyp_report_quick_overview.pdf",
                "https://www.cse.ust.hk/ct/fyp/reports/ethics_essay.html",
                "https://www.cse.ust.hk/ct/fyp/reports/content/conference.html"
            )
        ));

        pages.add(new MockPage(
            "FYP Report Content Guide",
            "https://www.cse.ust.hk/ct/fyp/reports/content/abstract.html",
            "Thu, 29 Apr 2021 00:00:00 GMT",
            11984L,
            keywords(
                keyword("fyp", 11),
                keyword("report", 10),
                keyword("abstract", 8),
                keyword("content", 7),
                keyword("poster", 4)
            ),
            links(
                "https://www.cse.ust.hk/ct/fyp/",
                "https://www.cse.ust.hk/ct/fyp/poster.html"
            ),
            links(
                "https://www.cse.ust.hk/ct/fyp/reports/content/ieee_style.html",
                "https://www.cse.ust.hk/ct/fyp/reports/content/conference.html",
                "https://www.cse.ust.hk/ct/fyp/reports/content/fyp_report_quick_overview.pdf"
            )
        ));

        pages.add(new MockPage(
            "HKUST Computer Science and Engineering",
            "https://www.cse.ust.hk/",
            "Tue, 26 Apr 2026 16:30:00 GMT",
            8420L,
            keywords(
                keyword("hkust", 11),
                keyword("computer", 10),
                keyword("science", 10),
                keyword("engineering", 7),
                keyword("research", 5)
            ),
            links("https://www.ust.hk/"),
            links(
                "https://www.cse.ust.hk/pg/",
                "https://www.cse.ust.hk/ug/",
                "https://www.cse.ust.hk/research/"
            )
        ));

        pages.add(new MockPage(
            "Information Technology Services",
            "https://itso.hkust.edu.hk/",
            "Fri, 23 Apr 2026 14:55:00 GMT",
            7310L,
            keywords(
                keyword("information", 9),
                keyword("technology", 9),
                keyword("services", 6),
                keyword("campus", 3),
                keyword("support", 3)
            ),
            links("https://www.ust.hk/"),
            links(
                "https://itso.hkust.edu.hk/services/",
                "https://itso.hkust.edu.hk/help/"
            )
        ));

        pages.add(new MockPage(
            "Hong Kong University Resources",
            "https://www.ust.hk/about-hkust/",
            "Thu, 22 Apr 2026 10:20:00 GMT",
            4688L,
            keywords(
                keyword("hong", 7),
                keyword("kong", 7),
                keyword("university", 6),
                keyword("resources", 5),
                keyword("campus", 4)
            ),
            links("https://www.ust.hk/"),
            links(
                "https://www.ust.hk/academics/",
                "https://www.ust.hk/life-hkust/"
            )
        ));

        return pages;
    }

    private static List<KeywordFreq> keywords(KeywordFreq... keywords) {
        return Arrays.asList(keywords);
    }

    private static List<String> links(String... links) {
        return Arrays.asList(links);
    }

    private static KeywordFreq keyword(String term, int freq) {
        return new KeywordFreq(term, freq);
    }

    private static final class QueryParts {
        private final List<String> phrases;
        private final List<String> terms;
        private final List<String> excludedTerms;

        private QueryParts(List<String> phrases, List<String> terms, List<String> excludedTerms) {
            this.phrases = phrases;
            this.terms = terms;
            this.excludedTerms = excludedTerms;
        }

        private boolean isEmpty() {
            return phrases.isEmpty() && terms.isEmpty();
        }
    }

    public static final class SearchResponse {
        private final String query;
        private final int totalResults;
        private final List<SearchResultItem> results;

        public SearchResponse(String query, int totalResults, List<SearchResultItem> results) {
            this.query = query;
            this.totalResults = totalResults;
            this.results = results;
        }

        public String getQuery() {
            return query;
        }

        public int getTotalResults() {
            return totalResults;
        }

        public List<SearchResultItem> getResults() {
            return results;
        }
    }

    public static final class SearchResultItem {
        private final double score;
        private final String title;
        private final String url;
        private final String lastModified;
        private final long sizeBytes;
        private final List<KeywordFreq> keywords;
        private final List<String> parentLinks;
        private final List<String> childLinks;

        public SearchResultItem(double score, String title, String url, String lastModified, long sizeBytes,
                                List<KeywordFreq> keywords, List<String> parentLinks, List<String> childLinks) {
            this.score = score;
            this.title = title;
            this.url = url;
            this.lastModified = lastModified;
            this.sizeBytes = sizeBytes;
            this.keywords = keywords;
            this.parentLinks = parentLinks;
            this.childLinks = childLinks;
        }

        public double getScore() {
            return score;
        }

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }

        public String getLastModified() {
            return lastModified;
        }

        public long getSizeBytes() {
            return sizeBytes;
        }

        public List<KeywordFreq> getKeywords() {
            return keywords;
        }

        public List<String> getParentLinks() {
            return parentLinks;
        }

        public List<String> getChildLinks() {
            return childLinks;
        }
    }

    public static final class KeywordFreq {
        private final String term;
        private final int freq;

        public KeywordFreq(String term, int freq) {
            this.term = term;
            this.freq = freq;
        }

        public String getTerm() {
            return term;
        }

        public int getFreq() {
            return freq;
        }
    }

    private static final class MockPage {
        private final String title;
        private final String url;
        private final String lastModified;
        private final long sizeBytes;
        private final List<KeywordFreq> keywords;
        private final List<String> parentLinks;
        private final List<String> childLinks;

        private MockPage(String title, String url, String lastModified, long sizeBytes, List<KeywordFreq> keywords,
                         List<String> parentLinks, List<String> childLinks) {
            this.title = title;
            this.url = url;
            this.lastModified = lastModified;
            this.sizeBytes = sizeBytes;
            this.keywords = keywords;
            this.parentLinks = parentLinks;
            this.childLinks = childLinks;
        }
    }
}

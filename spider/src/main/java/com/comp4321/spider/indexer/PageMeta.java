package com.comp4321.spider.indexer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializable metadata for a single crawled page, stored in the JDBM
 * "pageMetadata" HTree keyed by pageId (Integer).
 */
public class PageMeta implements Serializable {
    private static final long serialVersionUID = 1L;

    public int pageId;
    public String url = "";
    public String title = "";
    /** RFC-1123 formatted date string, e.g. "Sat, 29 Mar 2026 04:00:00 GMT" */
    public String lastModifiedRfc1123 = "";
    /** Page size in bytes */
    public long sizeBytes;
    public double maxTermFrequency;
    public double docNorm;

    /** Up to 10 child (out-link) URLs */
    public List<String> childUrls = new ArrayList<>();
    /** Parent page URLs (pages that link to this page) */
    public List<String> parentUrls = new ArrayList<>();

    /**
     * Top body stems by frequency (up to 10), ordered descending by frequency.
     * Key = stem string, Value = term frequency in the body.
     */
    public Map<String, Integer> topBodyStems = new LinkedHashMap<>();

    /**
     * Returns the L2 norm of the term frequency vector for this document.
     * Used for cosine similarity computation in ranking.
     */
    public double getNorm() {
        double sumSq = 0.0;
        for (Integer freq : topBodyStems.values()) {
            sumSq += freq * freq;
        }
        return Math.sqrt(sumSq);
    }

    /**
     * Returns the maximum term frequency in this document.
     * Used for TF normalization in ranking (term frequency / max term frequency).
     */
    public double getMaxTermFrequency() {
        if (topBodyStems.isEmpty()) {
            return 1.0;
        }
        return topBodyStems.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(1);
    }
}

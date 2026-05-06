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

    public double getMaxTermFrequency() {
        return maxTermFrequency;
    }

    public double getNorm() {
        // TODO: Store this value during indexing
        // For now, return 1.0 to keep cosine similarity simple
        return docNorm;
    }
}

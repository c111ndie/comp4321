package com.comp4321.spider.store;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CrawlState {
    public int nextPageId = 1;
    public Map<String, Integer> urlToPageId = new LinkedHashMap<>();
    public Map<Integer, PageRecord> pages = new LinkedHashMap<>();

    public CrawlState() {}
}


package com.comp4321.spider.store;

import java.util.LinkedHashSet;
import java.util.Set;

public final class PageRecord {
    public int pageId;
    public String url;
    public String title;
    public String lastModifiedRfc1123;
    public boolean lastModifiedFromHeader;
    public long sizeBytes;
    public Set<String> outLinks = new LinkedHashSet<>();
    public Set<Integer> parentPageIds = new LinkedHashSet<>();
    public boolean isHtml;

    public PageRecord() {
    }

    public PageRecord(int pageId, String url) {
        this.pageId = pageId;
        this.url = url;
    }
}

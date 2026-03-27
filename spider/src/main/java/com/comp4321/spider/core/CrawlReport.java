package com.comp4321.spider.core;

public final class CrawlReport {
    public final int pagesProcessed;
    public final int pagesDiscovered;

    public CrawlReport(int pagesProcessed, int pagesDiscovered) {
        this.pagesProcessed = pagesProcessed;
        this.pagesDiscovered = pagesDiscovered;
    }
}


package com.comp4321.spider.http;

import java.time.Instant;

public final class FetchHints {
    public final Instant ifModifiedSince;

    public FetchHints(Instant ifModifiedSince) {
        this.ifModifiedSince = ifModifiedSince;
    }

    public static FetchHints none() {
        return new FetchHints(null);
    }
}


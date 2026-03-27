package com.comp4321.spider.http;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class FetchResult {
    public final URI url;
    public final int statusCode;
    public final String contentType;
    public final byte[] bodyBytes;
    public final Map<String, List<String>> headers;
    public final Instant fetchTime;

    public FetchResult(
            URI url,
            int statusCode,
            String contentType,
            byte[] bodyBytes,
            Map<String, List<String>> headers,
            Instant fetchTime
    ) {
        this.url = url;
        this.statusCode = statusCode;
        this.contentType = contentType;
        this.bodyBytes = bodyBytes;
        this.headers = headers;
        this.fetchTime = fetchTime;
    }

    public boolean isNotModified() {
        return statusCode == 304;
    }
}


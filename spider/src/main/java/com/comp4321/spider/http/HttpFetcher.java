package com.comp4321.spider.http;

import com.comp4321.spider.util.HttpDates;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class HttpFetcher {
    private final HttpClient client;
    private final Duration requestTimeout;
    private final String userAgent;

    public HttpFetcher(Duration requestTimeout, String userAgent) {
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.requestTimeout = requestTimeout;
        this.userAgent = userAgent;
    }

    public FetchResult fetch(URI url, FetchHints hints) throws IOException, InterruptedException {
        Instant now = Instant.now();
        HttpRequest.Builder builder = HttpRequest.newBuilder(url)
                .timeout(requestTimeout)
                .GET()
                .header("User-Agent", userAgent)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");

        if (hints != null && hints.ifModifiedSince != null) {
            builder.header("If-Modified-Since", HttpDates.formatRfc1123(hints.ifModifiedSince));
        }

        HttpResponse<byte[]> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        String contentType = response.headers().firstValue("Content-Type").orElse(null);
        Map<String, List<String>> headers = response.headers().map();
        return new FetchResult(url, response.statusCode(), contentType, response.body(), headers, now);
    }

    public static Optional<String> headerFirstValue(Map<String, List<String>> headers, String name) {
        if (headers == null) {
            return Optional.empty();
        }
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) {
                List<String> values = e.getValue();
                if (values != null && !values.isEmpty()) {
                    return Optional.ofNullable(values.get(0));
                }
            }
        }
        return Optional.empty();
    }
}


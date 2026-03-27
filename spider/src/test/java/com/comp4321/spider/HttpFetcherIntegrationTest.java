package com.comp4321.spider;

import static org.junit.jupiter.api.Assertions.*;

import com.comp4321.spider.http.FetchHints;
import com.comp4321.spider.http.HttpFetcher;
import com.comp4321.spider.util.HttpDates;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class HttpFetcherIntegrationTest {
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void supportsIfModifiedSince_and_304() throws Exception {
        Instant lastModified = Instant.parse("2020-01-01T00:00:00Z");

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.createContext("/page", ex -> handlePage(ex, lastModified));
        server.start();

        URI url = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/page");
        HttpFetcher fetcher = new HttpFetcher(Duration.ofSeconds(5), "test-agent");

        var r1 = fetcher.fetch(url, FetchHints.none());
        assertEquals(200, r1.statusCode);
        assertNotNull(r1.bodyBytes);

        var r2 = fetcher.fetch(url, new FetchHints(lastModified.plusSeconds(3600)));
        assertEquals(304, r2.statusCode);
    }

    private static void handlePage(HttpExchange ex, Instant lastModified) throws IOException {
        String ifMod = ex.getRequestHeaders().getFirst("If-Modified-Since");
        if (ifMod != null) {
            var parsed = HttpDates.parseRfc1123(ifMod);
            if (parsed.isPresent() && !lastModified.isAfter(parsed.get())) {
                ex.getResponseHeaders().add("Last-Modified", HttpDates.formatRfc1123(lastModified));
                ex.sendResponseHeaders(304, -1);
                ex.close();
                return;
            }
        }

        byte[] body = "<html><head><title>x</title></head><body>ok</body></html>".getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
        ex.getResponseHeaders().add("Last-Modified", HttpDates.formatRfc1123(lastModified));
        ex.sendResponseHeaders(200, body.length);
        ex.getResponseBody().write(body);
        ex.close();
    }
}


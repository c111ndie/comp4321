package com.comp4321.spider.core;

import com.comp4321.spider.http.FetchHints;
import com.comp4321.spider.http.FetchResult;
import com.comp4321.spider.http.HttpFetcher;
import com.comp4321.spider.labs.Crawler;
import com.comp4321.spider.labs.Lab2Crawler;
import com.comp4321.spider.output.SpiderResultWriter;
import com.comp4321.spider.store.PageRecord;
import com.comp4321.spider.store.PageStore;
import com.comp4321.spider.util.HttpDates;
import com.comp4321.spider.util.UrlCanonicalizer;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class Spider {
    private final SpiderConfig config;
    private final HttpFetcher fetcher;
    private final Crawler crawler;
    private final Lab2Crawler lab2Crawler;

    public Spider(SpiderConfig config) {
        this.config = config;
        this.fetcher = new HttpFetcher(Duration.ofSeconds(30), config.userAgent);
        this.crawler = new Crawler();
        this.lab2Crawler = new Lab2Crawler();
    }

    public CrawlReport crawl() throws IOException, InterruptedException {
        PageStore store = new PageStore(config.outDir);
        Frontier frontier = new Frontier();

        Optional<URI> seed = UrlCanonicalizer.canonicalize(config.seed);
        if (seed.isEmpty()) {
            throw new IllegalArgumentException("Seed URL must be absolute http(s): " + config.seed);
        }
        frontier.enqueue(seed.get());

        int processedThisRun = 0;
        int uniqueCount = store.pageCount();

        while (!frontier.isEmpty()) {
            URI url = frontier.dequeue().orElse(null);
            if (url == null) {
                break;
            }
            if (!config.scopePolicy.allows(url)) {
                continue;
            }

            Optional<PageRecord> existing = store.getByUrl(url);
            if (existing.isEmpty() && uniqueCount >= config.maxPages) {
                continue;
            }

            FetchHints hints = existing.flatMap(store::lastModifiedInstant).map(FetchHints::new).orElse(FetchHints.none());

            FetchResult result;
            try {
                result = fetcher.fetch(url, hints);
            } catch (IOException e) {
                continue;
            }

            if (result.isNotModified()) {
                existing.ifPresent(r -> {
                    if (r.outLinks == null) {
                        return;
                    }
                    for (String s : r.outLinks) {
                        try {
                            URI child = URI.create(s);
                            if (config.scopePolicy.allows(child)) {
                                frontier.enqueue(child);
                            }
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                });
                processedThisRun++;
                continue;
            }

            if (result.statusCode < 200 || result.statusCode >= 300) {
                continue;
            }

            PageRecord record = existing.orElseGet(() -> {
                PageRecord created = store.getOrCreate(url);
                return created;
            });
            if (existing.isEmpty()) {
                uniqueCount++;
            }

            boolean isHtml = isHtmlContentType(result.contentType);
            record.isHtml = isHtml;

            if (isHtml) {
                Charset charset = charsetFromContentType(result.contentType).orElse(StandardCharsets.UTF_8);
                String html = new String(result.bodyBytes == null ? new byte[0] : result.bodyBytes, charset);
                store.saveHtml(record, html);

                record.title = crawler.extractTitle(html);

                long sizeBytes = contentLength(result)
                        .orElse(result.bodyBytes == null ? 0L : (long) result.bodyBytes.length);
                record.sizeChars = sizeBytes;

                LastModified chosen = chooseLastModified(result);
                record.lastModifiedRfc1123 = HttpDates.formatRfc1123(chosen.instant == null ? result.fetchTime : chosen.instant);
                record.lastModifiedFromHeader = chosen.fromLastModifiedHeader;

                Set<URI> outLinks = extractLinksLab2Style(url, html);
                record.outLinks.clear();
                for (URI link : outLinks) {
                    if (!config.scopePolicy.allows(link)) {
                        continue;
                    }
                    record.outLinks.add(link.toString());
                    frontier.enqueue(link);
                }
            } else {
                long sizeBytes = contentLength(result)
                        .orElse(result.bodyBytes == null ? 0L : (long) result.bodyBytes.length);
                record.sizeChars = sizeBytes;
                LastModified chosen = chooseLastModified(result);
                record.lastModifiedRfc1123 = HttpDates.formatRfc1123(chosen.instant == null ? result.fetchTime : chosen.instant);
                record.lastModifiedFromHeader = chosen.fromLastModifiedHeader;
                record.title = (record.title == null) ? "" : record.title;
                record.outLinks.clear();
            }

            store.recomputeParentLinks();
            store.checkpoint();
            processedThisRun++;

            if (!config.politenessDelay.isZero()) {
                Thread.sleep(config.politenessDelay.toMillis());
            }
        }

        if (store.pageCount() == 0) {
            System.err.println("Spider warning: no pages were saved. Check that the seed URL is reachable and returns HTTP 2xx HTML.");
        }
        store.recomputeParentLinks();
        Path outPath = store.getOutDir().resolve("spider_result.txt");
        new SpiderResultWriter().write(outPath, store.pagesByIdAscending());
        store.checkpoint();
        return new CrawlReport(processedThisRun, frontier.seenCount());
    }

    private Set<URI> extractLinksLab2Style(URI pageUrl, String htmlFallback) {
        java.util.LinkedHashSet<URI> out = new java.util.LinkedHashSet<>();
        try {
            var links = lab2Crawler.extractLinks(pageUrl.toString());
            for (String s : links) {
                UrlCanonicalizer.resolveAndCanonicalize(pageUrl, s).ifPresent(out::add);
            }
            return out;
        } catch (Exception ignored) {
            return new java.util.LinkedHashSet<>(crawler.extractLinks(htmlFallback, pageUrl));
        }
    }

    private static boolean isHtmlContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        String ct = contentType.toLowerCase(Locale.ROOT);
        return ct.startsWith("text/html") || ct.startsWith("application/xhtml+xml");
    }

    private static Optional<Long> contentLength(FetchResult result) {
        return HttpFetcher.headerFirstValue(result.headers, "Content-Length").flatMap(v -> {
            try {
                return Optional.of(Long.parseLong(v.trim()));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        });
    }

    private static final class LastModified {
        final Instant instant;
        final boolean fromLastModifiedHeader;

        LastModified(Instant instant, boolean fromLastModifiedHeader) {
            this.instant = instant;
            this.fromLastModifiedHeader = fromLastModifiedHeader;
        }
    }

    private static LastModified chooseLastModified(FetchResult result) {
        Optional<Instant> lm = HttpFetcher.headerFirstValue(result.headers, "Last-Modified").flatMap(HttpDates::parseRfc1123);
        if (lm.isPresent()) {
            return new LastModified(lm.get(), true);
        }
        Optional<Instant> date = HttpFetcher.headerFirstValue(result.headers, "Date").flatMap(HttpDates::parseRfc1123);
        return new LastModified(date.orElse(null), false);
    }

    private static Optional<Charset> charsetFromContentType(String contentType) {
        if (contentType == null) {
            return Optional.empty();
        }
        String[] parts = contentType.split(";");
        for (String p : parts) {
            String s = p.trim().toLowerCase(Locale.ROOT);
            if (s.startsWith("charset=")) {
                String cs = p.trim().substring("charset=".length()).trim();
                cs = cs.replace("\"", "");
                try {
                    return Optional.of(Charset.forName(cs));
                } catch (Exception ignored) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }
}

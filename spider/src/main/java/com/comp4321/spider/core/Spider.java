package com.comp4321.spider.core;

import com.comp4321.spider.http.FetchHints;
import com.comp4321.spider.http.FetchResult;
import com.comp4321.spider.http.HttpFetcher;
import com.comp4321.spider.store.PageRecord;
import com.comp4321.spider.store.PageStore;
import com.comp4321.spider.util.HttpDates;
import com.comp4321.spider.util.UrlCanonicalizer;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TitleTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

public final class Spider {
    private final SpiderConfig config;
    private final HttpFetcher fetcher;
    /** The PageStore from the most recent call to {@link #crawl()}. */
    private PageStore lastStore;

    public Spider(SpiderConfig config) {
        this.config = config;
        this.fetcher = new HttpFetcher(Duration.ofSeconds(30), config.userAgent);
    }

    /**
     * Returns the PageStore used during the last {@link #crawl()} call, or
     * {@code null} if crawl has not been called yet.
     */
    public PageStore getLastStore() {
        return lastStore;
    }

    public CrawlReport crawl() throws IOException, InterruptedException {
        this.lastStore = new PageStore(config.outDir);
        PageStore store = this.lastStore;
        Frontier frontier = new Frontier();

        Optional<URI> seed = UrlCanonicalizer.canonicalize(config.seed);
        if (seed.isEmpty()) {
            throw new IllegalArgumentException("Seed URL must be absolute http(s): " + config.seed);
        }
        frontier.enqueue(seed.get());

        int processedThisRun = 0;
        Set<Integer> coveredPageIds = new LinkedHashSet<>();

        while (!frontier.isEmpty()) {
            URI url = frontier.dequeue().orElse(null);
            if (url == null) {
                break;
            }
            if (!config.scopePolicy.allows(url)) {
                continue;
            }

            Optional<PageRecord> existing = store.getByUrl(url);
            // Once 30 BFS slots are filled, skip every further URL (new or existing).
            // Existing pages not covered this run will be evicted as "31st page" entries.
            if (coveredPageIds.size() >= config.maxPages) {
                continue;
            }

            FetchHints hints = existing.flatMap(store::lastModifiedInstant).map(FetchHints::new)
                    .orElse(FetchHints.none());

            FetchResult result;
            try {
                result = fetcher.fetch(url, hints);
            } catch (IOException e) {
                // Retain existing pages on transient network errors so they are not evicted.
                existing.ifPresent(r -> coveredPageIds.add(r.pageId));
                continue;
            }

            if (result.isNotModified()) {
                existing.ifPresent(r -> {
                    coveredPageIds.add(r.pageId);
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

            PageRecord record = existing.orElseGet(() -> store.getOrCreate(url));
            coveredPageIds.add(record.pageId);

            boolean isHtml = isHtmlContentType(result.contentType);
            record.isHtml = isHtml;

            if (isHtml) {
                Charset charset = charsetFromContentType(result.contentType).orElse(StandardCharsets.UTF_8);
                String html = new String(result.bodyBytes == null ? new byte[0] : result.bodyBytes, charset);
                store.saveHtml(record, html);

                record.title = extractTitle(html);

                long sizeBytes = contentLength(result)
                        .orElse(result.bodyBytes == null ? 0L : (long) result.bodyBytes.length);
                record.sizeBytes = sizeBytes;

                LastModified chosen = chooseLastModified(result);
                record.lastModifiedRfc1123 = HttpDates
                        .formatRfc1123(chosen.instant == null ? result.fetchTime : chosen.instant);
                record.lastModifiedFromHeader = chosen.fromLastModifiedHeader;

                Set<URI> outLinks = extractLinks(url, html, charset);
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
                record.sizeBytes = sizeBytes;
                LastModified chosen = chooseLastModified(result);
                record.lastModifiedRfc1123 = HttpDates
                        .formatRfc1123(chosen.instant == null ? result.fetchTime : chosen.instant);
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
            System.err.println(
                    "Spider warning: no pages were saved. Check that the seed URL is reachable and returns HTTP 2xx HTML.");
        }

        // Evict pages that were pushed beyond BFS position 30 (the "remove 31st page" rule).
        // Any DB page not confirmed in coveredPageIds this run was displaced by a newly
        // inserted page appearing earlier in BFS order.
        if (!coveredPageIds.isEmpty()) {
            Set<Integer> idsToRemove = new LinkedHashSet<>(store.pagesByIdAscending().keySet());
            idsToRemove.removeAll(coveredPageIds);
            for (Integer pageId : idsToRemove) {
                store.removePage(pageId);
            }
        }

        store.recomputeParentLinks();
        store.checkpoint();
        return new CrawlReport(processedThisRun, frontier.seenCount());
    }

    private Set<URI> extractLinks(URI pageUrl, String html, Charset charset) {
        LinkedHashSet<URI> out = new LinkedHashSet<>();
        if (html == null || html.isBlank()) {
            return out;
        }
        try {
            Parser parser = Parser.createParser(html, charset.name());
            NodeList nodes = parser.extractAllNodesThatMatch(new NodeClassFilter(LinkTag.class));
            for (Node node : nodes.toNodeArray()) {
                if (node instanceof LinkTag) {
                    String href = ((LinkTag) node).extractLink();
                    if (href != null && !href.isBlank()) {
                        UrlCanonicalizer.resolveAndCanonicalize(pageUrl, href).ifPresent(out::add);
                    }
                }
            }
        } catch (ParserException e) {
            // best-effort: return whatever links were collected before the error
        }
        return out;
    }

    private static String extractTitle(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        try {
            Parser parser = Parser.createParser(html, StandardCharsets.UTF_8.name());
            NodeList nodes = parser.extractAllNodesThatMatch(new NodeClassFilter(TitleTag.class));
            for (Node node : nodes.toNodeArray()) {
                if (node instanceof TitleTag) {
                    String title = ((TitleTag) node).getTitle();
                    return (title == null) ? "" : title.trim();
                }
            }
            return "";
        } catch (ParserException e) {
            return "";
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
        Optional<Instant> lm = HttpFetcher.headerFirstValue(result.headers, "Last-Modified")
                .flatMap(HttpDates::parseRfc1123);
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

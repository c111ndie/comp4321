package com.comp4321.spider.cli;

import com.comp4321.spider.core.ScopePolicy;
import com.comp4321.spider.core.Spider;
import com.comp4321.spider.core.SpiderConfig;
import com.comp4321.spider.indexer.JdbmIndexer;
import com.comp4321.spider.store.PageRecord;
import com.comp4321.spider.store.PageStore;
import org.htmlparser.Parser;
import org.htmlparser.visitors.TextExtractingVisitor;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Main {
    public static void main(String[] args) throws Exception {
        Map<String, String> parsed = parseArgs(args);
        if (parsed.containsKey("help") || !parsed.containsKey("seed") || !parsed.containsKey("max-pages") || !parsed.containsKey("out")) {
            printUsage();
            if (!parsed.containsKey("help")) {
                System.exit(2);
            }
            return;
        }

        URI seed = URI.create(parsed.get("seed"));
        int maxPages = Integer.parseInt(parsed.get("max-pages"));
        Path outDir = Path.of(parsed.get("out"));
        String dbName = parsed.getOrDefault("db-name", "indexDB");
        String stopwords = parsed.getOrDefault("stopwords", "stopwords.txt");

        Duration politenessDelay = Duration.ofMillis(Long.parseLong(parsed.getOrDefault("delay-ms", "0")));
        String userAgent = parsed.getOrDefault("user-agent", "comp4321-spider/1.0");

        // --- Crawl ---
        SpiderConfig config = new SpiderConfig(seed, maxPages, outDir, politenessDelay, userAgent, ScopePolicy.sameHostOnly(seed));
        Spider spider = new Spider(config);
        spider.crawl();
        PageStore store = spider.getLastStore();

        System.out.println("Crawl complete. Indexed " + store.pageCount() + " pages. Starting JDBM indexing...");

        // --- Index into JDBM ---
        JdbmIndexer indexer = new JdbmIndexer(dbName, stopwords);
        for (PageRecord page : store.pagesByIdAscending().values()) {
            if (!page.isHtml) continue;

            String html = store.readHtml(page).orElse("");
            String bodyText = extractBodyText(html);

            // Resolve parent URLs from parent pageIds
            List<String> parentUrls = new ArrayList<>();
            for (int parentId : page.parentPageIds) {
                store.getByPageId(parentId).ifPresent(p -> parentUrls.add(p.url));
            }

            List<String> childUrls = new ArrayList<>(page.outLinks);

            indexer.indexPage(
                    page.pageId,
                    page.url,
                    page.title != null ? page.title : "",
                    bodyText,
                    page.lastModifiedRfc1123,
                    page.sizeBytes,
                    childUrls,
                    parentUrls
            );
        }
        indexer.close();
        System.out.println("JDBM indexing complete. Database: " + dbName);
    }

    // -------------------------------------------------------------------------

    /** Extracts visible plain text from an HTML string using htmlparser. */
    static String extractBodyText(String html) {
        if (html == null || html.isBlank()) return "";
        try {
            Parser parser = Parser.createParser(html, "UTF-8");
            TextExtractingVisitor visitor = new TextExtractingVisitor();
            parser.visitAllNodesWith(visitor);
            return visitor.getExtractedText();
        } catch (Exception e) {
            // Fallback: strip all HTML tags
            return html.replaceAll("<[^>]+>", " ");
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar spider.jar --seed <url> --max-pages <n> --out <dir> [options]");
        System.out.println("  --seed        Starting URL");
        System.out.println("  --max-pages   Max pages to fetch (BFS)");
        System.out.println("  --out         Output directory for crawl state and HTML");
        System.out.println("  --db-name     JDBM database name (default: indexDB)");
        System.out.println("  --stopwords   Path to stopwords file (default: stopwords.txt)");
        System.out.println("  --delay-ms    Politeness delay between requests (default: 0)");
        System.out.println("  --user-agent  User-Agent header (default: comp4321-spider/1.0)");
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> out = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (!a.startsWith("--")) continue;
            String key = a.substring(2);
            if (key.equals("help")) { out.put("help", "true"); continue; }
            if (i + 1 >= args.length) break;
            out.put(key, args[++i]);
        }
        return out;
    }
}


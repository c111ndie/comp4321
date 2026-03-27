package com.comp4321.spider.cli;

import com.comp4321.spider.core.ScopePolicy;
import com.comp4321.spider.core.Spider;
import com.comp4321.spider.core.SpiderConfig;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
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

        Duration politenessDelay = Duration.ofMillis(Long.parseLong(parsed.getOrDefault("delay-ms", "0")));
        String userAgent = parsed.getOrDefault("user-agent", "comp4321-spider/1.0");

        SpiderConfig config = new SpiderConfig(seed, maxPages, outDir, politenessDelay, userAgent, ScopePolicy.sameHostOnly(seed));
        Spider spider = new Spider(config);
        spider.crawl();
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar spider.jar --seed <url> --max-pages <n> --out <dir> [--delay-ms <ms>] [--user-agent <ua>]");
        System.out.println("  --seed       Starting URL");
        System.out.println("  --max-pages  Max pages to fetch (BFS)");
        System.out.println("  --out        Output directory");
        System.out.println("  --delay-ms   Politeness delay between requests (default 0)");
        System.out.println("  --user-agent User-Agent header (default comp4321-spider/1.0)");
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> out = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (!a.startsWith("--")) {
                continue;
            }
            String key = a.substring(2);
            if (key.equals("help")) {
                out.put("help", "true");
                continue;
            }
            if (i + 1 >= args.length) {
                break;
            }
            out.put(key, args[++i]);
        }
        return out;
    }
}

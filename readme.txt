COMP4321 Phase 1 (Spider Only)

This submission includes the spider/crawler implementation (no indexer, no test program).

Requirements:
- Java 11+ (JDK)

Build (uses Maven Wrapper; Maven does NOT need to be installed):
Option A (from repo root):
1) ./mvnw -q test
2) ./mvnw -q package

Option B (from spider folder):
1) cd spider
2) ./mvnw -q test
3) ./mvnw -q package

Run:
cd spider
java -jar target/spider-1.0.0.jar \
  --seed https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm \
  --max-pages 30 \
  --out out

Notes:
- Crawl strategy is BFS and restricted to the same host as the seed URL.
- Re-running the spider reuses out/state.json and uses If-Modified-Since when Last-Modified was recorded.

Outputs (under the chosen --out directory):
- pages/page<pageId>.html    Raw HTML of fetched pages
- state.json                Persistent crawl state (URL<->pageId, metadata, outLinks, parentPageIds)
- spider_result.txt          Phase-1 style output (keyword line left blank since indexer is out of scope)

COMP4321 Phase 1 (Spider Only)

This submission includes the spider/crawler implementation (no indexer, no test program).

Requirements:
- Java 11+ (JDK)

macOS note:
- If `java -version` says "Unable to locate a Java Runtime", install a JDK and set JAVA_HOME.
- Homebrew example:
  - brew install openjdk@11
  - export JAVA_HOME="/opt/homebrew/opt/openjdk@11/libexec/openjdk.jdk/Contents/Home"
  - export PATH="$JAVA_HOME/bin:$PATH"

Build (uses Maven Wrapper; Maven does NOT need to be installed):
1) cd spider
2) ./mvnw -q clean package

Run (from the spider folder):
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

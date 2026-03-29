# COMP4321 Spider (no indexer)

This repository implements the **spider/crawler** part (BFS crawl) of the COMP4321 project, **without** the indexer.

## Requirements

- Java 11+
- No system Maven required (uses Maven Wrapper)

## Java setup (macOS)

On macOS, `java` may be a stub unless a JDK is installed.

If you use Homebrew:

```bash
brew install openjdk@11
export JAVA_HOME="/opt/homebrew/opt/openjdk@11/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

## Build

Run from the `spider/` directory:

```bash
cd spider
./mvnw -q clean package
```

## Run

```bash
java -jar target/spider-1.0.0.jar \
  --seed https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm \
  --max-pages 30 \
  --out out
```

Outputs:

- `out/pages/page<pageId>.html` (raw HTML)
- `out/state.json` (crawl state, includes `parentPageIds`)

Notes:

- Crawl strategy is BFS and restricted to the same host as the seed URL.
- Link extraction parses `<a href>` tags from already-downloaded HTML using HTMLParser (no second HTTP request).

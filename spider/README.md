# COMP4321 Spider (no indexer)

This repository implements the **spider/crawler** part (BFS crawl) of the COMP4321 project, **without** the indexer.

## Requirements

- Java 11+
- No system Maven required (uses Maven Wrapper)

## Build

```bash
./mvnw -q test
./mvnw -q package
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
- `out/spider_result.txt` (phase-1 style output; keyword line is blank)

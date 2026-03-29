# COMP4321 Spider + Indexer

This module implements the **spider/crawler** (BFS crawl) and **JDBM indexer** for the COMP4321 Phase 1 project.

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

## Java setup (Windows)

1. Download and install [OpenJDK 11](https://adoptium.net/temurin/releases/?version=11) (Temurin JDK 11, `.msi` installer).
2. The installer sets `JAVA_HOME` and `PATH` automatically. Verify in a new Command Prompt or PowerShell:

```powershell
java -version
```

## Build

All commands below must be run from **inside the `spider/` directory** (i.e. `cd spider` first if you cloned the repo and are at the root).

### macOS / Linux / Git Bash (Windows)

```bash
./mvnw -q clean package
```

### Windows — Command Prompt

```cmd
mvnw.cmd -q clean package
```

### Windows — PowerShell

```powershell
.\mvnw.cmd -q clean package
```


## Run: Crawl, Index, and Export Results


### 0. Compile (The code has already been compiled)


### 1. Crawl and Index (macOS / Linux / Git Bash / Windows)

```bash
cd /workspaces/comp4321/spider
./mvnw clean package -DskipTests

java -jar target/spider-1.0.0.jar \
  --seed https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm \
  --max-pages 30 \
  --out crawl-output \
  --db-name indexDB \
  --stopwords stopwords.txt

java -cp "target/spider-1.0.0.jar:../txt_builder" SearchResultsExporter crawl-output indexDB crawl-output/spider_result.txt
```

**Parameters:**
- `crawl-output`: Path to the PageStore (contains `state.json` and `pages/`)
- `indexDB`: Path to the JDBM database created by the spider
- `crawl-output/spider_result.txt`: Output filename for the results

**Output:**
- Reads all crawled pages from PageStore
- Extracts keywords and frequencies from JDBM database for each page
- Generates `spider_result.txt` with all pages and their keywords

**Success message:**
```
✅ Exported 30 pages to: spider_result.txt
✅ Results exported successfully
```

**View the results:**
```bash
cat crawl-output/spider_result.txt
```

### Output Format

Each page entry contains:
```
[Page Title]
[URL]
[Last Modified Date], [Size in bytes]
[Keyword1] [Freq1]; [Keyword2] [Freq2]; ...
[Child Link 1]
[Child Link 2]
...

========================================================================
[Next Page]
...
```


### Windows — Command Prompt or PowerShell

```cmd
java -jar target\spider-1.0.0.jar --seed https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm --max-pages 30 --out crawl-output --db-name indexDB --stopwords stopwords.txt
```

### Options

| Flag | Default | Description |
|---|---|---|
| `--seed` | _(required)_ | Starting URL |
| `--max-pages` | _(required)_ | Max pages to fetch (BFS) |
| `--out` | _(required)_ | Output directory for crawl state and raw HTML |
| `--db-name` | `indexDB` | JDBM database name (`<name>.db` / `<name>.lg` created in working dir) |
| `--stopwords` | `stopwords.txt` | Path to stopwords file (falls back to the bundled one in the JAR) |
| `--delay-ms` | `0` | Politeness delay between requests (ms) |
| `--user-agent` | `comp4321-spider/1.0` | HTTP User-Agent header |

### Outputs

- `crawl-output/pages/page<id>.html` — raw HTML of each fetched page
- `crawl-output/state.json` — crawl state (URLs, titles, dates, parent/child links)
- `indexDB.db` + `indexDB.lg` — JDBM database (submit these for Phase 1)

### JDBM Database Schema

| HTree | Key | Value | Purpose |
|---|---|---|---|
| `urlToPageId` | `String` URL | `Integer` pageId | URL → ID lookup |
| `pageIdToUrl` | `Integer` pageId | `String` URL | ID → URL lookup |
| `wordToWordId` | `String` stem | `Integer` wordId | Stem → word ID |
| `wordIdToWord` | `Integer` wordId | `String` stem | Word ID → stem |
| `bodyInvertedIndex` | `Integer` wordId | `PostingList` | Body inverted index (positions for phrase search) |
| `titleInvertedIndex` | `Integer` wordId | `PostingList` | Title inverted index (positions for phrase search) |
| `forwardIndex` | `Integer` pageId | `Map<wordId,freq>` | Forward index (body term frequencies for tf·idf) |
| `pageMetadata` | `Integer` pageId | `PageMeta` | Title, URL, date, size, top keywords, child/parent URLs |
| `counters` | `String` | `Integer` | Internal counters (`nextWordId`) |

Notes:

- Crawl strategy is BFS, restricted to the same host as the seed URL.
- Pages are re-fetched only if the server reports a newer `Last-Modified` date.
- Stop words are removed and remaining words are stemmed with Porter's algorithm before indexing.
- `PostingList` stores per-document token positions, enabling phrase search (e.g. `"hong kong"`).

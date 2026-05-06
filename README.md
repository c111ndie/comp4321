# COMP4321 Spider + Indexer + Test Program

This module implements the **spider/crawler** (BFS crawl) and **JDBM indexer** for the COMP4321 Phase 1 project.

## Requirements

- Preferably MacOS
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


### 1. Crawl, Index, Generate results (macOS / Linux / Git Bash)

```bash
cd spider
./mvnw clean package -DskipTests

java -jar target/spider-1.0.0.jar \
  --seed https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm \
  --max-pages 30 \
  --out crawl-output \
  --db-name indexDB \
  --stopwords stopwords.txt

cd ..
mkdir -p txt_builder/build
javac -cp spider/target/spider-1.0.0.jar -d txt_builder/build txt_builder/*.java
java -cp "spider/target/spider-1.0.0.jar:txt_builder/build" SearchResultsExporter spider/crawl-output spider/indexDB spider/crawl-output/spider_result.txt
```

**Parameters:**
- `spider/crawl-output`: Path to the PageStore (contains `state.json` and `pages/`)
- `spider/indexDB`: Path to the JDBM database created by the spider
- `spider/crawl-output/spider_result.txt`: Output filename for the results

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
cat spider/crawl-output/spider_result.txt
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
cd spider
mvnw.cmd clean package -DskipTests

java -jar target\spider-1.0.0.jar --seed https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm --max-pages 30 --out crawl-output --db-name indexDB --stopwords stopwords.txt

cd ..
if not exist txt_builder\build mkdir txt_builder\build
javac -cp spider\target\spider-1.0.0.jar -d txt_builder\build txt_builder\*.java
java -cp "spider\target\spider-1.0.0.jar;txt_builder\build" SearchResultsExporter spider\crawl-output spider\indexDB spider\crawl-output\spider_result.txt
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

---

## Web Interface (Final Submission)

The `webapp/` module is a Spring Boot application that serves a search UI on `http://localhost:8080` and exposes JSON endpoints used by the frontend.

All commands below are run from the **project root** unless stated otherwise.

### Step 1 — Build everything (spider + webapp)

```bash
# macOS / Linux
./mvnw clean package -DskipTests

# Windows
mvnw.cmd clean package -DskipTests
```

### Step 2 — Crawl and index pages

```bash
# macOS / Linux
java -jar spider/target/spider-1.0.0.jar \
  --seed https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm \
  --max-pages 30 \
  --out spider/crawl-output \
  --db-name spider/crawl-output/indexDB \
  --stopwords spider/src/main/resources/stopwords.txt
```

```cmd
:: Windows
java -jar spider\target\spider-1.0.0.jar --seed https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm --max-pages 30 --out spider\crawl-output --db-name spider\crawl-output\indexDB --stopwords spider\src\main\resources\stopwords.txt
```

This produces `spider/crawl-output/indexDB.db` (the JDBM database).

### Step 3 — Start the web server

```bash
# macOS / Linux
java \
  -Dsearch.db-name=spider/crawl-output/indexDB \
  -Dsearch.stopwords=spider/src/main/resources/stopwords.txt \
  -jar webapp/target/webapp-1.0.0.jar
```

```cmd
:: Windows
java -Dsearch.db-name=spider\crawl-output\indexDB -Dsearch.stopwords=spider\src\main\resources\stopwords.txt -jar webapp\target\webapp-1.0.0.jar
```

By default, the webapp starts with temporary mock endpoints enabled:

- `/api/search`
- `/api/suggest`

This lets the UI be previewed before the real search engine backend is merged. To disable the mock endpoints once the real implementation is ready, set:

```properties
webapp.mock-search.enabled=false
```

in `webapp/src/main/resources/application.properties`.

### Step 4 — Open the search engine

| URL | Purpose |
|---|---|
| `http://localhost:8080/index.html` | Homepage |
| `http://localhost:8080/results.html?q=hong+kong` | Results page |
| `http://localhost:8080/api/search?q=hong+kong` | JSON API |
| `http://localhost:8080/api/suggest?q=comp` | Autocomplete API |

To change the port append `--server.port=9090` to the Step 3 command.

### Development preview mode

Use this mode while editing the web interface. It runs the Spring Boot webapp from source, so UI changes are easier to preview than when running the packaged JAR.

```bash
# macOS / Linux, from the project root
export JAVA_HOME="/opt/homebrew/opt/openjdk@11/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

./mvnw -f webapp/pom.xml \
  org.springframework.boot:spring-boot-maven-plugin:2.7.18:run \
  -Dspring-boot.run.arguments=--server.port=8081
```

Then open:

```text
http://localhost:8081/index.html
```

If port `8080` is free, you can remove `-Dspring-boot.run.arguments=--server.port=8081` and use the default `http://localhost:8080`.

### Previewing the frontend without the backend

The HTML/CSS/JS files are plain static — no build step needed. Serve them directly with Python:

```bash
cd webapp/src/main/resources/static
python3 -m http.server 3000
# → http://localhost:3000/index.html
```

> **Note:** Search and autocomplete call `/api/search` and `/api/suggest`. When the Spring Boot server is running, the current project includes temporary mock implementations for both endpoints so the UI can be previewed before backend integration.

### Features

- Homepage: branded header, search bar, sample query chips, history panel
- Results page: ranked cards with score, title, URL, date, size, keywords, parent/child links
- Phrase search: `"hong kong" university`
- Multiple keywords: `computer science`
- Combined: `"computer science" hkust`
- Excluded terms: `university -private`
- Query history stored in browser `localStorage` (persists across reloads)
- Autocomplete suggestions fetched from `/api/suggest`
- Active filter chips showing quoted phrases and excluded terms
- Collapsible parent/child link section per result card
- Get similar pages by resubmitting the top displayed keywords from a result
- Saved results stored in browser `localStorage`
- Search within the current result set without another backend request
- Responsive layout, keyboard-friendly, loading / empty / error states

# COMP4321 Search Engine — Group 32

A web-based search engine built with a BFS spider, JDBM inverted index, TF×IDF/cosine-similarity ranking, and a Spring Boot frontend.

---

## Requirements

- **Java 11** (JDK, not just JRE)
- No system Maven needed — Maven Wrapper (`mvnw` / `mvnw.cmd`) is included

---

## 1. Java Setup

### macOS

If you installed Java via Homebrew:

```bash
brew install openjdk@11
export JAVA_HOME="$(dirname $(dirname $(which java)))"
export PATH="$JAVA_HOME/bin:$PATH"
java -version   # should print: openjdk version "11.x.x"
```

Add the `export` lines to `~/.zshrc` (or `~/.bash_profile`) to make them permanent.

### Linux

```bash
# Debian / Ubuntu
sudo apt install openjdk-11-jdk

# Fedora / RHEL
sudo dnf install java-11-openjdk-devel

# Verify
java -version
```

If you have multiple JDKs:

```bash
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
export PATH="$JAVA_HOME/bin:$PATH"
```

### Windows

1. Download and install [Eclipse Temurin JDK 11](https://adoptium.net/temurin/releases/?version=11) (`.msi` installer).
   The installer sets `JAVA_HOME` and updates `PATH` automatically.
2. Open a **new** Command Prompt or PowerShell and verify:

```powershell
java -version
```

---

## 2. Build

Run all commands from the **project root** (the folder containing this `README.md`).

### macOS / Linux

```bash
./mvnw clean package -DskipTests
```

### Windows — Command Prompt

```cmd
mvnw.cmd clean package -DskipTests
```

### Windows — PowerShell

```powershell
.\mvnw.cmd clean package -DskipTests
```

This compiles both the `spider` and `webapp` modules and places JARs in `spider/target/` and `webapp/target/`.

---

## 3. Crawl and Index Pages

Run from the **project root**.

### macOS / Linux

```bash
java -jar spider/target/spider-1.0.0.jar \
  --seed https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm \
  --max-pages 30 \
  --out spider/crawl-output \
  --db-name spider/crawl-output/indexDB \
  --stopwords spider/src/main/resources/stopwords.txt
```

### Windows — Command Prompt

```cmd
java -jar spider\target\spider-1.0.0.jar --seed https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm --max-pages 30 --out spider\crawl-output --db-name spider\crawl-output\indexDB --stopwords spider\src\main\resources\stopwords.txt
```

### Windows — PowerShell

```powershell
java -jar spider\target\spider-1.0.0.jar `
  --seed https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm `
  --max-pages 30 `
  --out spider\crawl-output `
  --db-name spider\crawl-output\indexDB `
  --stopwords spider\src\main\resources\stopwords.txt
```

**Expected output:**

```
Crawl complete. Indexed 30 pages. Starting JDBM indexing...
JDBM indexing complete. Database: spider/crawl-output/indexDB
```

This creates `spider/crawl-output/indexDB.db` and `indexDB.lg` — the JDBM index files.

> To index 300 pages for the final submission, change `--max-pages 30` to `--max-pages 300`.

### Spider flags

| Flag | Default | Description |
|---|---|---|
| `--seed` | _(required)_ | Starting URL |
| `--max-pages` | _(required)_ | Max pages to fetch (BFS) |
| `--out` | _(required)_ | Output directory for crawl state and raw HTML |
| `--db-name` | `indexDB` | JDBM database path (without extension) |
| `--stopwords` | `stopwords.txt` | Path to stopwords file |
| `--delay-ms` | `0` | Politeness delay between requests (ms) |
| `--user-agent` | `comp4321-spider/1.0` | HTTP User-Agent header |

---

## 4. Start the Web Server

Run from the **project root** after completing Step 3.

### macOS / Linux

```bash
java -jar webapp/target/webapp-1.0.0.jar
```

### Windows — Command Prompt

```cmd
java -jar webapp\target\webapp-1.0.0.jar
```

### Windows — PowerShell

```powershell
java -jar webapp\target\webapp-1.0.0.jar
```

The server starts on **port 8080**. You should see in the logs:

```
[RealSearchController] JDBM index loaded from: spider/crawl-output/indexDB
```

If you see an error about "JDBM named objects not found", re-run Step 3 first.

---

## 5. Open the Search Engine

| URL | Purpose |
|---|---|
| `http://localhost:8080` | Homepage |
| `http://localhost:8080/results.html?q=hong+kong` | Results page |
| `http://localhost:8080/api/search?q=hong+kong` | JSON search API |
| `http://localhost:8080/api/suggest?q=comp` | Autocomplete API |
| `http://localhost:8080/api/keywords` | Full indexed keyword list API |

---

## 6. Generate `spider_result.txt` (Phase 1)

Run from the **project root** after the spider has finished (Step 3).

### macOS / Linux

```bash
mkdir -p txt_builder/build
javac -cp spider/target/spider-1.0.0.jar -d txt_builder/build txt_builder/*.java
java -cp "spider/target/spider-1.0.0.jar:txt_builder/build" SearchResultsExporter \
  spider/crawl-output \
  spider/crawl-output/indexDB \
  spider/crawl-output/spider_result.txt
```

### Windows — Command Prompt

```cmd
if not exist txt_builder\build mkdir txt_builder\build
javac -cp spider\target\spider-1.0.0.jar -d txt_builder\build txt_builder\*.java
java -cp "spider\target\spider-1.0.0.jar;txt_builder\build" SearchResultsExporter spider\crawl-output spider\crawl-output\indexDB spider\crawl-output\spider_result.txt
```

Output file: `spider/crawl-output/spider_result.txt`

### Output format

```
Page Title
URL
Last Modified Date, Size in bytes
Keyword1 freq1; Keyword2 freq2; ...
Child Link 1
Child Link 2
...
----------------------------------------------------------------------
```

---

## Troubleshooting

| Problem | Fix |
|---|---|
| `JAVA_HOME` not set / `./mvnw` fails | Run `export JAVA_HOME="$(dirname $(dirname $(which java)))"` (macOS/Linux) |
| Port 8080 already in use | Add `--server.port=9090` to the `java -jar` command |
| "JDBM named objects not found" | Re-run Step 3 (index files are missing or were built by an old version) |
| Search returns no results | Make sure you ran the spider from the **project root** so the db path `spider/crawl-output/indexDB` is correct |

---

## JDBM Database Schema

| HTree | Key | Value | Purpose |
|---|---|---|---|
| `urlToPageId` | `String` URL | `Integer` pageId | URL → ID lookup |
| `pageIdToUrl` | `Integer` pageId | `String` URL | ID → URL lookup |
| `wordToWordId` | `String` stem | `Integer` wordId | Stem → word ID |
| `wordIdToWord` | `Integer` wordId | `String` stem | Word ID → stem |
| `bodyInvertedIndex` | `Integer` wordId | `PostingList` | Body inverted index with positions (phrase search) |
| `titleInvertedIndex` | `Integer` wordId | `PostingList` | Title inverted index with positions |
| `forwardIndex` | `Integer` pageId | `Map<wordId,freq>` | Body term frequencies for TF·IDF |
| `pageMetadata` | `Integer` pageId | `PageMeta` | Title, URL, date, size, top keywords, child/parent URLs |
| `counters` | `String` | `Integer` | Internal counters (`nextWordId`) |

---

## Features

- BFS crawler with politeness delay, Last-Modified caching, cycle detection
- Porter stemming + stop word removal before indexing
- Two inverted indexes (body + title) with token positions for phrase search
- TF×IDF / max(TF) weighting with cosine similarity ranking
- Title match boost (TITLE_BOOST = 1000)
- Phrase search: `"hong kong" university`
- Excluded terms: `university -private`
- Autocomplete suggestions
- Browse all indexed (stemmed) keywords and build queries visually (infinite scroll)
- Query history and saved results (browser `localStorage`)
- "Get similar pages" — re-queries using the top keywords of a result
- Responsive UI with loading / empty / error states

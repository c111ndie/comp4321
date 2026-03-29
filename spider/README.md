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

## Run

### macOS / Linux / Git Bash (Windows)

```bash
java -jar target/spider-1.0.0.jar \
  --seed https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm \
  --max-pages 30 \
  --out out
```

### Windows — Command Prompt or PowerShell

```cmd
java -jar target\spider-1.0.0.jar --seed https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm --max-pages 30 --out out
```

Outputs:

- `out/pages/page<pageId>.html` (raw HTML)
- `out/state.json` (crawl state, includes `parentPageIds`)

Notes:

- Crawl strategy is BFS and restricted to the same host as the seed URL.
- Link extraction parses `<a href>` tags from already-downloaded HTML using HTMLParser (no second HTTP request).

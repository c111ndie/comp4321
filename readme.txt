COMP4321 Spider + Indexer
-----------------------------------------------------------

This submission contains the spider/crawler and JDBM indexer for COMP4321 Phase 1 under the spider/ folder.

Requirements
- Java 11+
- No system Maven required (uses Maven Wrapper)

Java setup (macOS)
- If java is not available, install a JDK first.
- Homebrew example:
  - brew install openjdk@11
  - export JAVA_HOME="/opt/homebrew/opt/openjdk@11/libexec/openjdk.jdk/Contents/Home"
  - export PATH="$JAVA_HOME/bin:$PATH"
  - java -version

Java setup (Windows)
- Install OpenJDK 11 (for example, Temurin JDK 11).
- Verify in Command Prompt or PowerShell with:
  - java -version

Build
- All commands below must be run from inside the spider/ directory.

macOS / Linux / Git Bash:
1) cd spider
2) ./mvnw -q clean package

Windows Command Prompt:
1) cd spider
2) mvnw.cmd -q clean package

Windows PowerShell:
1) cd spider
2) .\mvnw.cmd -q clean package

Run

macOS / Linux / Git Bash:
1) cd spider
2) java -jar target/spider-1.0.0.jar \
     --seed https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm \
     --max-pages 30 \
     --out crawl-output \
     --db-name indexDB \
     --stopwords stopwords.txt

Windows Command Prompt / PowerShell:
1) cd spider
2) java -jar target\spider-1.0.0.jar --seed https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm --max-pages 30 --out crawl-output --db-name indexDB --stopwords stopwords.txt


Outputs
- spider/crawl-output/pages/page<id>.html: raw HTML of each fetched page
- spider/crawl-output/state.json: crawl state including URLs, titles, dates, and parent/child links
- spider/indexDB.db and spider/indexDB.lg: JDBM database files

Test Program
---------------------------------------------------------------


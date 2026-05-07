More readable version in COMP4321/README.md

Requirements
- Preferably MacOS
- Java 11+
- No system Maven required (uses Maven Wrapper)

JAVA SETUP (macOS)
- If java is not available, install a JDK first.
If you use Homebrew:

brew install openjdk@11
export JAVA_HOME="/opt/homebrew/opt/openjdk@11/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
java -version

JAVA SETUP (Windows)
Download and install OpenJDK 11 (Temurin JDK 11, .msi installer).
The installer sets JAVA_HOME and PATH automatically. Verify in a new Command Prompt or PowerShell:

java -version

BUILD
- All commands below must be run from inside the spider/ directory (i.e. cd spider first if you cloned the repo and are at the root).

macOS / Linux / Git Bash:

./mvnw -q clean package

Windows Command Prompt:

mvnw.cmd -q clean package

Windows PowerShell:

.\mvnw.cmd -q clean package

RUN: Crawl, Index, and Export Results

macOS / Linux / Git Bash:

cd spider
./mvnw clean package -DskipTests

java -jar target/spider-1.0.0.jar \
  --seed https://hitcslj.github.io/TestPages/testpage.htm \
  --max-pages 300 \
  --out crawl-output \
  --db-name indexDB \
  --stopwords stopwords.txt

cd ..
mkdir -p txt_builder/build
javac -cp spider/target/spider-1.0.0.jar -d txt_builder/build txt_builder/*.java
java -cp "spider/target/spider-1.0.0.jar:txt_builder/build" SearchResultsExporter spider/crawl-output spider/indexDB spider/crawl-output/spider_result.txt

Windows Command Prompt:

cd spider
mvnw.cmd clean package -DskipTests

java -jar target\spider-1.0.0.jar --seed https://hitcslj.github.io/TestPages/testpage.htm --max-pages 300 --out crawl-output --db-name indexDB --stopwords stopwords.txt

cd ..
if not exist txt_builder\build mkdir txt_builder\build
javac -cp spider\target\spider-1.0.0.jar -d txt_builder\build txt_builder\*.java
java -cp "spider\target\spider-1.0.0.jar;txt_builder\build" SearchResultsExporter spider\crawl-output spider\indexDB spider\crawl-output\spider_result.txt

Windows PowerShell (with line continuation using backticks):

cd spider
.\mvnw.cmd clean package -DskipTests

java -jar target\spider-1.0.0.jar `
  --seed https://hitcslj.github.io/TestPages/testpage.htm `
  --max-pages 300 `
  --out crawl-output `
  --db-name indexDB `
  --stopwords stopwords.txt

cd ..
if (-not (Test-Path txt_builder\build)) { New-Item -ItemType Directory -Path txt_builder\build | Out-Null }
javac -cp spider\target\spider-1.0.0.jar -d txt_builder\build txt_builder\*.java
java -cp "spider\target\spider-1.0.0.jar;txt_builder\build" SearchResultsExporter spider\crawl-output spider\indexDB spider\crawl-output\spider_result.txt

Outputs
- Reads all crawled pages from PageStore
- Extracts keywords and frequencies from the JDBM database for each page
- Generates spider_result.txt in spider/crawl-output/

================================================================================
WEB INTERFACE (Final Submission)
================================================================================

OVERVIEW
The web interface is a Spring Boot application in the webapp/ directory.
It serves a dark-navy/gold-themed search UI on http://localhost:8080
and exposes a JSON search API at /api/search.

The search engine is backed by the real JDBM index built by the spider.
You MUST complete Step 2 (crawl) before Step 3 (run server).

All commands below are run from the project root unless stated otherwise.

--------------------------------------------------------------------------------
STEP 1 — Build everything (spider + webapp)
--------------------------------------------------------------------------------

  ./mvnw clean package -DskipTests          # macOS / Linux
  mvnw.cmd clean package -DskipTests        # Windows

--------------------------------------------------------------------------------
STEP 2 — Crawl and index pages
--------------------------------------------------------------------------------

macOS / Linux:
  java -jar spider/target/spider-1.0.0.jar \
    --seed https://hitcslj.github.io/TestPages/testpage.htm \
    --max-pages 300 \
    --out spider/crawl-output \
    --db-name spider/crawl-output/indexDB \
    --stopwords spider/src/main/resources/stopwords.txt

Windows:
  java -jar spider\target\spider-1.0.0.jar --seed https://hitcslj.github.io/TestPages/testpage.htm --max-pages 300 --out spider\crawl-output --db-name spider\crawl-output\indexDB --stopwords spider\src\main\resources\stopwords.txt

This produces: spider/crawl-output/indexDB.db  (the JDBM database)

--------------------------------------------------------------------------------
STEP 3 — Start the web server (Spring Boot + frontend)
--------------------------------------------------------------------------------

macOS / Linux:
  java \
    -Dsearch.db-name=spider/crawl-output/indexDB \
    -Dsearch.stopwords=spider/src/main/resources/stopwords.txt \
    -jar webapp/target/webapp-1.0.0.jar

Windows:
  java -Dsearch.db-name=spider\crawl-output\indexDB -Dsearch.stopwords=spider\src\main\resources\stopwords.txt -jar webapp\target\webapp-1.0.0.jar

--------------------------------------------------------------------------------
STEP 4 — Open the search engine
--------------------------------------------------------------------------------

  Homepage:    http://localhost:8080/index.html
  Results:     http://localhost:8080/results.html?q=hong+kong
  JSON API:    http://localhost:8080/api/search?q=hong+kong

--------------------------------------------------------------------------------
PREVIEWING THE FRONTEND WITHOUT THE BACKEND
--------------------------------------------------------------------------------

The HTML/CSS/JS files are plain static files. You can preview the UI layout
without running the Java server using Python's built-in HTTP server:

  cd webapp/src/main/resources/static
  python3 -m http.server 3000

Then open: http://localhost:3000/index.html

Note: search and autocomplete will show an error state because the
/api/search and /api/suggest endpoints require the Spring Boot server (Step 3).
Everything else — homepage layout, history panel, filter chips, navigation —
works fully without the backend.

--------------------------------------------------------------------------------
FEATURES
--------------------------------------------------------------------------------
- Homepage: branded header, search bar, sample query chips, history panel
- Results page: ranked cards with score, title, URL, date, size, keywords, links
- Phrase search:   "hong kong" university
- Excluded terms:  university -private
- Combined:        "computer science" hkust -biology
- Query history stored in browser localStorage (persists across reloads)
- Autocomplete suggestions fetched from indexed page titles
- Active filter chips showing active phrases and exclusions
- Collapsible parent/child link section per result card
- Get similar pages by resubmitting the top displayed keywords from a result
- Saved results stored in browser localStorage
- Search within the current result set without another backend request
- Responsive layout, keyboard-friendly, loading/empty/error states

PORT
The server runs on port 8080 by default.
To change: append --server.port=9090 to the java command in Step 3.

--------------------------------------------------------------------------------
DEVELOPMENT PREVIEW MODE
--------------------------------------------------------------------------------

Use this mode while editing the web interface. It runs the Spring Boot webapp
from source, so UI changes are easier to preview than when running the packaged
JAR.

macOS / Linux, from the project root:
  export JAVA_HOME="/opt/homebrew/opt/openjdk@11/libexec/openjdk.jdk/Contents/Home"
  export PATH="$JAVA_HOME/bin:$PATH"

  ./mvnw -f webapp/pom.xml \
    org.springframework.boot:spring-boot-maven-plugin:2.7.18:run \
    -Dspring-boot.run.arguments=--server.port=8081

Then open:
  http://localhost:8081/index.html

If port 8080 is free, you can remove:
  -Dspring-boot.run.arguments=--server.port=8081

and use the default:
  http://localhost:8080

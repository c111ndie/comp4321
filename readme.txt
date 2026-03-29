More readable version in COMP4321/README.md

Requirements
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
  --seed https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm \
  --max-pages 30 \
  --out crawl-output \
  --db-name indexDB \
  --stopwords stopwords.txt

cd ..
mkdir -p txt_builder/build
javac -cp spider/target/spider-1.0.0.jar -d txt_builder/build txt_builder/*.java
java -cp "spider/target/spider-1.0.0.jar:txt_builder/build" SearchResultsExporter spider/crawl-output spider/indexDB spider/crawl-output/spider_result.txt

Windows Command Prompt / PowerShell:

cd spider
mvnw.cmd clean package -DskipTests

java -jar target\spider-1.0.0.jar --seed https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm --max-pages 30 --out crawl-output --db-name indexDB --stopwords stopwords.txt

cd ..
if not exist txt_builder\build mkdir txt_builder\build
javac -cp spider\target\spider-1.0.0.jar -d txt_builder\build txt_builder\*.java
java -cp "spider\target\spider-1.0.0.jar;txt_builder\build" SearchResultsExporter spider\crawl-output spider\indexDB spider\crawl-output\spider_result.txt

Outputs
- Reads all crawled pages from PageStore
- Extracts keywords and frequencies from the JDBM database for each page
- Generates spider_result.txt in spider/crawl-output/

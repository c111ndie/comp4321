# COMP4321 txt_builder

## Overview
This folder contains code for processing crawled webpage data, extracting keywords and frequencies from indexed databases, and exporting results to text files.

## Files
- `txtGenerator.java`: Core text output system with session-based buffering for efficient I/O.
- `DbKeywordExtractor.java`: Extracts keywords and their frequencies from JDBM database files.
- `SearchResultsExporter.java`: Integrates PageStore, DbKeywordExtractor, and Printer to export complete results.

## Usage

### Exporting Results from Database

After crawling with the spider (which generates the database and page store), you can extract and print the results as follows:

1. **Compile the Java files**:
   ```bash
   cd /workspaces/comp4321/txt_builder
   javac -cp "../../spider/target/spider-1.0.0.jar" *.java
   ```

2. **Run SearchResultsExporter**:
   ```bash
   cd /workspaces/comp4321/spider
   java -cp "target/spider-1.0.0.jar:../txt_builder" SearchResultsExporter crawl-output indexDB results.txt
   ```

   **Parameters**:
   - `crawl-output`: Path to the PageStore (contains `state.json` and `pages/`)
   - `indexDB`: Path to the JDBM database created by the spider
   - `results.txt`: Output filename for the results

3. **Output**:
   The command will:
   - Read all 30 crawled pages from PageStore
   - Extract keywords and frequencies from JDBM database for each page
   - Generate `results.txt` with all pages and their keywords
   
   Success message:
   ```
   ✅ Exported 30 pages to: results.txt
   ✅ Results exported successfully
   ```

4. **View the results**:
   ```bash
   cat results.txt
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

### Complete Pipeline Example

```bash
# 1. Build spider
cd /workspaces/comp4321/spider
./mvnw clean package -DskipTests

# 2. Run spider (crawls and indexes)
java -jar target/spider-1.0.0.jar \
    --seed https://www.cse.ust.hk/~kwtleung/COMP4321/testpage.htm \
    --max-pages 30 \
    --out crawl-output \
    --db-name indexDB \
    --stopwords stopwords.txt

# 3. Compile extraction tools
cd /workspaces/comp4321/txt_builder
javac -cp "../../spider/target/spider-1.0.0.jar" *.java

# 4. Export results with keywords
cd /workspaces/comp4321/spider
java -cp "target/spider-1.0.0.jar:../txt_builder" SearchResultsExporter crawl-output indexDB crawl-output/results.txt

# 5. View results
cat results.txt
```
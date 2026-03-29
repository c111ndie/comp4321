# COMP4321 Ethan Code

## Overview
This folder contains code for processing crawled webpage data, extracting keywords and frequencies from indexed databases, and exporting results to text files.

## Files
- `txtGenerator.java`: Core text output system with session-based buffering for efficient I/O.
- `DbKeywordExtractor.java`: Extracts keywords and their frequencies from JDBM database files.
- `SearchResultsExporter.java`: Integrates PageStore, DbKeywordExtractor, and Printer to export complete results.

## Usage

### Exporting Results from Database

After crawling with the spider (which generates the database and page store), you can extract and print the results as follows:

1. **Prerequisites**:
   - Ensure the spider has been run and generated the database (e.g., `indexDB`) and page store (e.g., in `crawl-output`).
   - Compile the Java files: `javac *.java`

2. **Run SearchResultsExporter**:
   ```
   java SearchResultsExporter <dbPath> <outputPath> <pageStorePath>
   ```

   - `<dbPath>`: Path to the JDBM database directory (e.g., `../spider/indexDB`)
   - `<outputPath>`: Directory where output txt files will be saved (e.g., `output`)
   - `<pageStorePath>`: Path to the PageStore directory containing crawled pages (e.g., `../spider/crawl-output`)

   **Example**:
   ```
   java SearchResultsExporter ../spider/indexDB output ../spider/crawl-output
   ```

   This command will:
   - Read all pages from the PageStore
   - Extract keywords and frequencies from the JDBM database for each page
   - Generate individual txt files in the `output` directory, each containing webpage metadata, keywords, and frequencies

3. **Output Format**:
   Each output file will contain:
   - Webpage URL, title, size, and other metadata
   - List of keywords with their frequencies
   - Formatted for easy reading and further processing

### Notes
- The code assumes integration with the spider's PageStore and indexer components.
- For large datasets, the session-based buffering in txtGenerator provides significant performance improvements.
- Ensure all required classes (from spider package) are available in the classpath during compilation and runtime.
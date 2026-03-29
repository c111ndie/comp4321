# Integration: Complete Workflow

## Pipeline Flow
1. **Spider** crawls pages and creates indexed JDBM database
2. **DbKeywordExtractor** reads the indexed database and extracts keywords/frequencies per page
3. **SearchResultsExporter** combines page metadata with keywords and outputs to text file using Printer

## Usage
```bash
# 1. Run spider (creates crawl-output/ and indexDB)
java -jar spider.jar --seed https://example.com --max-pages 30 --out crawl-output --db-name indexDB --stopwords stopwords.txt

# 2. Export results with keywords
java SearchResultsExporter crawl-output indexDB results.txt
```

# txtGenerator.java
It is just a program with two classes that allows writing the results into the required txt file.

I placed it in util file of the spider.

## WebpageData

This class packages the information required into a single object. This should permit 
### Variables
- boolean initialized: A tag that checks if WebpageData is initialized to facilitate sanity check
- String pageTitle
- String url
- String lastModData
- String sizeOfPage
- String[] keywords
- Object[] freq: This allows it to accept both int and String as input
- String[] childLinks

### Methods
- WebpageData(): Dummy Constructor
- WebpageData(String pageTitle, String url, String lastModDate, String sizeOfPage, String[] keywords, int[] freq, String[] childLinks): 
Constructor that receives all required data
- WebpageData(PageRecord page): Constructor that loads page record data.
- void loadWebpageData(String pageTitle, String url, String lastModDate, String sizeOfPage, String[] keywords, int[] freq, String[] childLinks): 
Loads required data
- boolean checkInitialized(): Checks if necessary fields have been filled
- void displayInfo(): Debugging method that prints content out

### Usage
Create a WebpageData instance, and  

## Printer

This class is assigned to a txt path, receives a content and write them to the file.

### Variables
- private String output: The output text file path
- initialized = false:  A tag that checks if WebpageData is initialized to facilitate sanity check
- private int entryCount: tracks number of written entries
- private BufferedWriter sessionWriter: buffer for batch writing
- private boolean inSession: whether batch session is active

### Methods
- Printer(String output_txt_file_name): Constructor. File path mandatory.
- void initTxtFile(): Clears the text file.
- void startSession(): Opens a buffered writer for batch mode.
- void endSession(): Flushes and closes batch writer.
- void appendWebpageData(WebpageData data): Writes content to end of file.
- void appendWebpageData(PageRecord data): Writes PageRecord content to end of file.
- void appendWebpageDataBatch(List<WebpageData> dataList): Batch append multiple WebpageData.
- void appendPageRecordBatch(List<PageRecord> dataList): Batch append multiple PageRecord.
- int getEntryCount(): Get total entries written.
- void resetEntryCount(): Reset entry count to zero.

# DbKeywordExtractor.java

Reads keywords and their frequencies from the JDBM indexed database for a specific page.

## Overview
- Opens and reads JDBM database created by Indexer
- Iterates through title and body indices
- Extracts keywords that appear in a specific document
- Returns frequencies with title-weighted boost (2x)

## Key Methods
- `DbKeywordExtractor(String dbName)`: Open database
- `KeywordFrequencyResult extractKeywordsForPage(int pageId)`: Extract keywords for a page
- `void close()`: Close database connection

## KeywordFrequencyResult
Container holding:
- `String[] keywords`: Array of extracted terms
- `Integer[] frequencies`: Array of occurrence counts

## Usage Example
```java
DbKeywordExtractor extractor = new DbKeywordExtractor("indexDB");
DbKeywordExtractor.KeywordFrequencyResult result = extractor.extractKeywordsForPage(1);

System.out.println("Page 1 keywords:");
for (int i = 0; i < result.keywords.length; i++) {
    System.out.println("  " + result.keywords[i] + ": " + result.frequencies[i]);
}

extractor.close();
```

# SearchResultsExporter.java

Integrates PageStore + DbKeywordExtractor + Printer to export complete search results with keywords to text file.

## Overview
- Reads page metadata from PageStore
- Extracts keywords from indexed database using DbKeywordExtractor  
- Outputs formatted results to text file using Printer
- Uses high-performance batch writing for efficiency

## Key Methods
- `SearchResultsExporter(Path pageStorePath, String dbName, String outputFile)`: Initialize exporter
- `int exportAllPages()`: Export all pages with keywords to file
- `void exportPageById(int pageId)`: Export single page
- `void close()`: Close all resources

## Output Format
Each page entry contains:
```
[Page Title]
[URL]
[Last Modified], [Size] bytes
[Keyword1] [Freq1]; [Keyword2] [Freq2]; ...
[Child Link 1]
[Child Link 2]
...
========================================================================
[Next Page]
```

## Usage Example
```bash
# Command line
java SearchResultsExporter ./crawl-output ./indexDB results.txt
```

```java
// Programmatic
SearchResultsExporter exporter = new SearchResultsExporter(
    Path.of("./crawl-output"),
    "./indexDB",
    "results.txt"
);

int count = exporter.exportAllPages();
exporter.close();

System.out.println("Exported " + count + " pages");
```

## Complete Pipeline Example
```bash
#!/bin/bash

# 1. Crawl pages with indexing
cd spider
java -jar target/spider-1.0.0.jar \
  --seed https://example.com \
  --max-pages 30 \
  --out crawl-output \
  --db-name indexDB \
  --stopwords stopwords.txt

# 2. Export results with keywords
cd ../Ethan/code
java SearchResultsExporter \
  ../../spider/crawl-output \
  ../../spider/indexDB \
  search_results.txt

echo "Results saved to search_results.txt"
```

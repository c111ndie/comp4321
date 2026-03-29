# Text generator.java
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
- WebpageData(PageRecord page, String[] keywords, String[] freq): Constructor that loads page record data and keyword data.
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
- private boolean empty: A tag that is true if the Printer has checked the target is empty

### Methods
- Printer(String output_txt_file_name): Constructor. FIle path mandatory.
- void initTxtFile(): Clears the text file.
- void appendWebpageData(WebpageData data): Writes content to the end of text file. Automatically adds separator if file to be written is not empty.
- void appendWebpageData(PageRecord data): Writes content to the end of text file. Automatically adds separator if file to be written is not empty.

# dbGenerator.java

Complete database generator for indexing and persisting crawled pages.

## Overview

The `dbGenerator` class integrates page crawling with full-text indexing to output indexed pages to a JDBM database file. It:
- Reads crawled pages from PageStore
- Processes and stems page content using StopStem
- Builds separate inverted indices for titles and body text
- Persists the complete index to a database file

## Key Features

1. **Separate Title/Body Indexing**: Pages are indexed with separate title and body indices, allowing weighted search results
2. **Stopword Filtering**: Removes common words during indexing for efficiency
3. **Porter Stemming**: Reduces terms to their root form for better matching
4. **JDBM Persistence**: All indexed data is stored in a JDBM database for efficient retrieval
5. **Summary Export**: Generates detailed summary of indexed pages

## Constructor

```java
public dbGenerator(Path pageStorePath, Path indexDbPath, Path stopwordsPath) throws IOException
```

- `pageStorePath`: Directory containing crawled pages (PageStore)
- `indexDbPath`: Directory where index database will be saved
- `stopwordsPath`: Path to stopwords file

## Main Methods

### Indexing
- `int indexAllPages()`: Index all pages in the store, returns count
- `boolean indexPageById(int pageId)`: Index a specific page by ID
- `boolean indexPage(PageRecord record)`: Index a single page record

### Searching
- `PostingList searchTitleTerm(String term)`: Find pages with term in title
- `PostingList searchBodyTerm(String term)`: Find pages with term in body

### Management
- `void close()`: Close database and persist all changes
- `void exportIndexSummary(Path outputPath)`: Write index summary to file
- `int getIndexedPageCount()`: Get total pages indexed
- `Set<Integer> getIndexedPageIds()`: Get all indexed page IDs

### Access to Core Components
- `Indexer getIndexer()`: Get underlying Indexer instance
- `PageStore getPageStore()`: Get underlying PageStore instance

## Usage Example

```java
// Initialize with paths
dbGenerator generator = new dbGenerator(
    Path.of("./pages"),           // PageStore directory
    Path.of("./indexDB"),         // Index database path
    Path.of("./stopwords.txt")    // Stopwords file
);

// Index all pages
int numPages = generator.indexAllPages();

// Export summary
generator.exportIndexSummary(Path.of("index_summary.txt"));

// Search for a term
PostingList results = generator.searchTitleTerm("information");

// Close and save
generator.close();
```

## Integration with Other Modules

This class integrates these external components (from COMP4321 and spider):
- **Indexer**: Handles tokenization and indexing (from COMP4321)
- **InvertedIndex**: JDBM-backed index storage (from COMP4321)
- **PostingList**: Term posting lists (from COMP4321)
- **StopStem**: Stopword filtering and stemming (from COMP4321)
- **PageStore**: Crawled page storage (from spider)
- **PageRecord**: Page metadata (from spider)

## Database Output

The index database file contains:
- **titleIndex**: Inverted index of page titles with term positions
- **bodyIndex**: Inverted index of page bodies with term positions
- Document frequency and position information for ranking

## Execution Flow

1. Create `dbGenerator` instance with paths
2. Call `indexAllPages()` to process all crawled pages
3. Each page's title and body are tokenized, stemmed, and stopwords removed
4. Terms are stored in separate indices with document IDs and positions
5. Index is persisted to database file via JDBM
6. Export summary for verification
7. Call `close()` to finalize and commit changes

## Output Files

- `indexed_pages.db` (or specified path): JDBM database containing the inverted indices
- `index_summary.txt`: Human-readable summary of indexed pages
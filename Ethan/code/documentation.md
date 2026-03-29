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

# dbGenerator.java

Full-text index generator that stores crawled pages into a JDBM-backed inverted index, and supports indexing, searching and summary export.

## Overview
- Works with PageStore / PageRecord from spider package.
- Uses Indexer, InvertedIndex, PostingList, StopStem from COMP4321 package.
- Builds separate title and body inverted indices with term positions.
- Persists data in a folder specified by indexDbPath.
- Supports export of index summary to text file.

## Core Functionality
- `dbGenerator(Path pageStorePath, Path indexDbPath, Path stopwordsPath)` : constructor.
- `int indexAllPages()` : indexes all pages present in PageStore.
- `boolean indexPageById(int pageId)` : indexes one page by its ID.
- `boolean indexPage(PageRecord record)` : index single record.
- `PostingList searchTitleTerm(String term)` : lookup title index postings.
- `PostingList searchBodyTerm(String term)` : lookup body index postings.
- `void exportIndexSummary(Path outputPath)` : write index summary to text.
- `void close()` : close underlying indexer and flush data.
- `Indexer getIndexer()` : access Indexer instance.
- `PageStore getPageStore()` : access PageStore instance.
- `int getIndexedPageCount()` : number of indexed pages.
- `Set<Integer> getIndexedPageIds()` : get IDs of indexed pages.

## Usage Example
```java
Path pageStorePath = Path.of("./pages");
Path indexDbPath = Path.of("./indexDB");
Path stopwordsPath = Path.of("./stopwords.txt");

dbGenerator generator = new dbGenerator(pageStorePath, indexDbPath, stopwordsPath);
int numIndexed = generator.indexAllPages();

generator.exportIndexSummary(Path.of("index_summary.txt"));

PostingList titleResults = generator.searchTitleTerm("information");
PostingList bodyResults = generator.searchBodyTerm("retriev");

generator.close();
```

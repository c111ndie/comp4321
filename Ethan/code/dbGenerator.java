import java.io.*;
import java.util.*;
import java.nio.file.Path;

// Assumes these will be available from integrated modules:
// - Indexer, InvertedIndex, PostingList, StopStem from COMP4321
// - PageStore, PageRecord from spider

/**
 * Database Generator for outputting indexed pages to a JDBM database.
 * Integrates page crawling, text processing, and index storage.
 * 
 * This class serves as the main entry point for:
 * 1. Reading crawled pages from PageStore
 * 2. Processing and stemming page content
 * 3. Building inverted indices (separate for titles and body)
 * 4. Storing indexed data persistently in a database file
 */
public class dbGenerator {
    
    private Indexer indexer;
    private PageStore pageStore;
    private Path indexDbPath;
    private Set<Integer> indexedPageIds;
    private Map<String, Integer> wordToWordId;
    private int wordIdCounter;

    /**
     * Initialize the database generator with paths to page store and index location.
     * 
     * @param pageStorePath Path to the crawled pages store directory
     * @param indexDbPath Path where the index database will be saved
     * @param stopwordsPath Path to stopwords file for filtering
     * @throws IOException if paths cannot be accessed or resources cannot be initialized
     */
    public dbGenerator(Path pageStorePath, Path indexDbPath, Path stopwordsPath) throws IOException {
        this.indexDbPath = indexDbPath;
        this.pageStore = new PageStore(pageStorePath);
        this.indexer = new Indexer(stopwordsPath.toString(), indexDbPath.toString());
        this.indexedPageIds = new HashSet<>();
        this.wordToWordId = new HashMap<>();
        this.wordIdCounter = 0;
    }

    /**
     * Index all pages from the PageStore into the database.
     * 
     * @return Number of pages successfully indexed
     * @throws IOException if indexing fails
     */
    public int indexAllPages() throws IOException {
        int count = 0;
        System.out.println("Starting indexing process...");
        
        for (PageRecord record : pageStore.getAllPages()) {
            if (indexPage(record)) {
                count++;
                if (count % 10 == 0) {
                    System.out.println("Indexed " + count + " pages...");
                }
            }
        }
        
        System.out.println("✅ Successfully indexed " + count + " pages");
        System.out.println("📁 Database saved to: " + indexDbPath);
        return count;
    }

    /**
     * Index a specific page by its ID.
     * 
     * @param pageId The page ID to index
     * @return true if successful, false otherwise
     * @throws IOException if indexing fails
     */
    public boolean indexPageById(int pageId) throws IOException {
        Optional<PageRecord> page = pageStore.getByPageId(pageId);
        if (page.isPresent()) {
            return indexPage(page.get());
        }
        return false;
    }

    /**
     * Index a single page record.
     * Processes title and body content separately for weighted searching.
     * 
     * @param record The PageRecord to index
     * @return true if indexed successfully, false otherwise
     * @throws IOException if indexing fails
     */
    public boolean indexPage(PageRecord record) throws IOException {
        if (record == null || record.title == null || record.title.isEmpty()) {
            return false;
        }

        // Use page ID as document identifier
        String docId = String.valueOf(record.pageId);
        String title = record.title;
        
        // Extract body content from stored HTML if available
        String body = extractBodyContent(record);

        // Index the document (title and body are indexed separately)
        indexer.indexDocument(docId, title, body);
        indexedPageIds.add(record.pageId);
        
        return true;
    }

    /**
     * Extract body text content from a page record.
     * In the integrated system, this would read actual HTML content from storage.
     * 
     * @param record The PageRecord to extract content from
     * @return Body text content
     * @throws IOException if content cannot be read
     */
    private String extractBodyContent(PageRecord record) throws IOException {
        StringBuilder body = new StringBuilder();
        
        // Add URL to body for indexing
        if (record.url != null && !record.url.isEmpty()) {
            body.append(record.url).append(" ");
        }
        
        // In integrated version, would read actual page HTML:
        // Optional<String> html = pageStore.readHtml(record);
        // if (html.isPresent()) {
        //     body.append(HtmlParser.extractText(html.get()));
        // }
        
        return body.toString();
    }

    /**
     * Get the underlying Indexer for direct access to indexed data.
     * 
     * @return The Indexer instance
     */
    public Indexer getIndexer() {
        return indexer;
    }

    /**
     * Get the underlying PageStore for direct access to page records.
     * 
     * @return The PageStore instance
     */
    public PageStore getPageStore() {
        return pageStore;
    }

    /**
     * Search for pages containing a term in titles.
     * 
     * @param term The term to search for
     * @return PostingList containing pages and positions, or null if not found
     * @throws IOException if search fails
     */
    public PostingList searchTitleTerm(String term) throws IOException {
        return indexer.getTitleTerm(term);
    }

    /**
     * Search for pages containing a term in body text.
     * 
     * @param term The term to search for
     * @return PostingList containing pages and positions, or null if not found
     * @throws IOException if search fails
     */
    public PostingList searchBodyTerm(String term) throws IOException {
        return indexer.getBodyTerm(term);
    }

    /**
     * Close the database and save all indexed data.
     * 
     * @throws IOException if database closure fails
     */
    public void close() throws IOException {
        indexer.close();
        System.out.println("✅ Index database closed and persisted");
    }

    /**
     * Export summary of indexed pages to text file.
     * 
     * @param outputPath Path to write the summary file
     * @throws IOException if export fails
     */
    public void exportIndexSummary(Path outputPath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath.toFile()))) {
            writer.println("=== Indexed Pages Summary ===");
            writer.println("Total pages indexed: " + indexedPageIds.size());
            writer.println("Index database: " + indexDbPath);
            writer.println("Generated: " + new java.util.Date());
            writer.println();
            writer.println("Indexed Page IDs and Details:");
            writer.println("=====================================");
            
            indexedPageIds.stream().sorted().forEach(pageId -> {
                try {
                    Optional<PageRecord> record = pageStore.getByPageId(pageId);
                    if (record.isPresent()) {
                        PageRecord r = record.get();
                        writer.println("\n[Page ID: " + pageId + "]");
                        writer.println("Title: " + r.title);
                        writer.println("URL: " + r.url);
                        writer.println("Last Modified: " + (r.lastModifiedRfc1123 != null ? r.lastModifiedRfc1123 : "N/A"));
                        writer.println("Size: " + r.sizeChars + " bytes");
                        writer.println("HTML: " + (r.isHtml ? "Yes" : "No"));
                        if (r.outLinks != null && !r.outLinks.isEmpty()) {
                            writer.println("Outgoing Links: " + r.outLinks.size());
                        }
                    }
                } catch (IOException e) {
                    writer.println("Error reading page " + pageId);
                }
            });
        }
        System.out.println("✅ Index summary exported to: " + outputPath);
    }

    /**
     * Get count of indexed pages.
     * 
     * @return Number of pages in index
     */
    public int getIndexedPageCount() {
        return indexedPageIds.size();
    }

    /**
     * Get all page IDs that were indexed.
     * 
     * @return Set of page IDs
     */
    public Set<Integer> getIndexedPageIds() {
        return Collections.unmodifiableSet(indexedPageIds);
    }

    /**
     * Main method demonstrating usage.
     * In integrated version, would:
     * 1. Call spider to crawl pages
     * 2. Use this to index the crawled pages
     * 3. Persist index to database
     * 
     * @param args Command line arguments (optional: page store path, index db path, stopwords path)
     */
    public static void main(String[] args) {
        try {
            // Default paths (will be customizable in integrated version)
            Path pageStorePath = Path.of("./pages");
            Path indexDbPath = Path.of("./indexDB");
            Path stopwordsPath = Path.of("./stopwords.txt");

            // Allow command line override
            if (args.length >= 1) pageStorePath = Path.of(args[0]);
            if (args.length >= 2) indexDbPath = Path.of(args[1]);
            if (args.length >= 3) stopwordsPath = Path.of(args[2]);

            // Create and run indexer
            dbGenerator generator = new dbGenerator(pageStorePath, indexDbPath, stopwordsPath);
            
            int numIndexed = generator.indexAllPages();
            generator.exportIndexSummary(Path.of("index_summary.txt"));
            generator.close();

            System.out.println("✅ Indexing pipeline complete!");
            System.out.println("   Pages indexed: " + numIndexed);
            System.out.println("   Output database: " + indexDbPath);

        } catch (IOException e) {
            System.err.println("❌ Error during indexing: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

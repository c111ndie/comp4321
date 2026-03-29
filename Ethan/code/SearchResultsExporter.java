import com.comp4321.spider.store.PageRecord;
import com.comp4321.spider.store.PageStore;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Integrates DbKeywordExtractor with txtGenerator Printer.
 * Exports search results with keywords/frequencies from the indexed database.
 * 
 * @author Integration
 * @version 27/3
 */
public class SearchResultsExporter {
    
    private final PageStore pageStore;
    private final DbKeywordExtractor extractor;
    private final Printer printer;

    /**
     * Initialize exporter.
     * 
     * @param pageStorePath Path to PageStore (crawled pages)
     * @param dbName Path to JDBM database (indexed pages)
     * @param outputFile Path to output text file
     * @throws IOException if resources cannot be opened
     */
    public SearchResultsExporter(Path pageStorePath, String dbName, String outputFile) throws IOException {
        this.pageStore = new PageStore(pageStorePath);
        this.extractor = new DbKeywordExtractor(dbName);
        this.printer = new Printer(outputFile);
        this.printer.initTxtFile();
    }

    /**
     * Export all pages with their keywords and frequencies.
     * 
     * @return Number of pages exported
     * @throws IOException if export fails
     */
    public int exportAllPages() throws IOException {
        int count = 0;
        printer.startSession();
        
        for (PageRecord record : pageStore.pagesByIdAscending().values()) {
            if (record != null && record.title != null && !record.title.isEmpty()) {
                exportPage(record);
                count++;
            }
        }
        
        printer.endSession();
        return count;
    }

    /**
     * Export a single page with keywords and frequencies from database.
     * 
     * @param pageId The page ID to export
     * @throws IOException if export fails
     */
    public void exportPageById(int pageId) throws IOException {
        Optional<PageRecord> record = pageStore.getByPageId(pageId);
        if (record.isPresent()) {
            exportPage(record.get());
        }
    }

    /**
     * Export single page record with keywords extracted from database.
     * 
     * @param record The page record to export
     * @throws IOException if export fails
     */
    private void exportPage(PageRecord record) throws IOException {
        // Extract keywords and frequencies from database
        DbKeywordExtractor.KeywordFrequencyResult kwResult = 
            extractor.extractKeywordsForPage(record.pageId);

        // Create WebpageData with keywords and frequencies
        WebpageData data = new WebpageData(
            record.title,
            record.url,
            (record.lastModifiedRfc1123 == null || record.lastModifiedRfc1123.isBlank()) 
                ? "N/A" : record.lastModifiedRfc1123,
            String.valueOf(record.sizeChars),
            kwResult.keywords,
            kwResult.frequencies,
            record.outLinks.toArray(new String[0])
        );

        // Write to file
        printer.appendWebpageData(data);
    }

    /**
     * Close all resources.
     * 
     * @throws IOException if closure fails
     */
    public void close() throws IOException {
        extractor.close();
        System.out.println("✅ Results exported successfully");
    }

    /**
     * Main: Export search results from database.
     * Usage: java SearchResultsExporter <pageStorePath> <dbName> <outputFile>
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("Usage: SearchResultsExporter <pageStorePath> <dbName> <outputFile>");
            System.out.println("  pageStorePath: Path to crawled pages directory");
            System.out.println("  dbName: Path to JDBM database file");
            System.out.println("  outputFile: Output text file for results");
            System.exit(1);
        }

        Path pageStorePath = Path.of(args[0]);
        String dbName = args[1];
        String outputFile = args[2];

        SearchResultsExporter exporter = new SearchResultsExporter(pageStorePath, dbName, outputFile);
        
        try {
            int count = exporter.exportAllPages();
            System.out.println("✅ Exported " + count + " pages to: " + outputFile);
        } finally {
            exporter.close();
        }
    }
}

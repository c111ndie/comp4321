import java.io.*;
import java.util.*;
import jdbm.*;
import jdbm.htree.HTree;
import jdbm.helper.FastIterator;
import com.comp4321.spider.indexer.PostingList;

/**
 * Extracts keywords and their frequencies from the JDBM index database.
 * Used to populate WebpageData with keyword information for output.
 * 
 * @author Integration
 * @version 27/3
 */
public class DbKeywordExtractor {
    
    private RecordManager recman;
    private HTree titleIndex;
    private HTree bodyIndex;
    private HTree wordIdToWord;

    /**
     * Initialize extractor with path to JDBM database.
     * 
     * @param dbName Path to the JDBM database (created by Indexer)
     * @throws IOException if database cannot be opened
     */
    public DbKeywordExtractor(String dbName) throws IOException {
        recman = RecordManagerFactory.createRecordManager(dbName);
        
        // Load or create indices
        long titleRecID = recman.getNamedObject("titleInvertedIndex");
        long bodyRecID = recman.getNamedObject("bodyInvertedIndex");
        long wordIdToWordRecID = recman.getNamedObject("wordIdToWord");
        
        System.out.println("titleRecID: " + titleRecID);
        System.out.println("bodyRecID: " + bodyRecID);
        System.out.println("wordIdToWordRecID: " + wordIdToWordRecID);
        
        // Debug other names
        System.out.println("urlToPageId: " + recman.getNamedObject("urlToPageId"));
        System.out.println("pageIdToUrl: " + recman.getNamedObject("pageIdToUrl"));
        System.out.println("wordToWordId: " + recman.getNamedObject("wordToWordId"));
        System.out.println("forwardIndex: " + recman.getNamedObject("forwardIndex"));
        
        if (titleRecID == 0 || bodyRecID == 0 || wordIdToWordRecID == 0) {
            System.out.println("Missing indices. title: " + titleRecID + ", body: " + bodyRecID + ", wordIdToWord: " + wordIdToWordRecID);
            throw new IOException("Database does not contain valid indices. Make sure it was created by Indexer.");
        }
        
        titleIndex = HTree.load(recman, titleRecID);
        bodyIndex = HTree.load(recman, bodyRecID);
        wordIdToWord = HTree.load(recman, wordIdToWordRecID);
    }

    /**
     * Extract keywords and frequencies for a specific page from the database.
     * 
     * @param pageId The page ID to extract keywords for
     * @return KeywordFrequencyResult containing keywords array and frequencies array
     * @throws IOException if database read fails
     */
    public KeywordFrequencyResult extractKeywordsForPage(int pageId) throws IOException {
        List<String> keywords = new ArrayList<>();
        List<Integer> frequencies = new ArrayList<>();
        
        // Extract from title index (higher weight)
        extractFromIndex(titleIndex, pageId, keywords, frequencies, 2.0); // 2x weight for title
        
        // Extract from body index
        extractFromIndex(bodyIndex, pageId, keywords, frequencies, 1.0);
        
        return new KeywordFrequencyResult(
            keywords.toArray(new String[0]),
            frequencies.toArray(new Integer[0])
        );
    }

    /**
     * Extract keywords from a specific index (title or body).
     * 
     * @param index The HTree index to search
     * @param pageId The page ID to find
     * @param keywords List to accumulate keywords
     * @param frequencies List to accumulate frequencies
     * @param weight Multiplier for frequency (e.g., 2.0 for title)
     * @throws IOException if iteration fails
     */
    private void extractFromIndex(HTree index, int pageId, List<String> keywords, 
                                   List<Integer> frequencies, double weight) throws IOException {
        FastIterator iterator = index.keys();
        Integer wordId;
        
        while ((wordId = (Integer) iterator.next()) != null) {
            PostingList postingList = (PostingList) index.get(wordId);
            String term = (String) wordIdToWord.get(wordId);
            
            if (postingList != null && term != null) {
                // Check if this page appears in the posting list
                Set<Integer> pageIds = postingList.getPageIds();
                if (pageIds.contains(pageId)) {
                    // Get frequency (number of times term appears in page)
                    List<Integer> positions = postingList.getPositions(pageId);
                    int frequency = (int) (positions.size() * weight);
                    
                    // Check if keyword already exists
                    int existingIndex = keywords.indexOf(term);
                    if (existingIndex >= 0) {
                        // Accumulate frequency
                        frequencies.set(existingIndex, frequencies.get(existingIndex) + frequency);
                    } else {
                        // New keyword
                        keywords.add(term);
                        frequencies.add(frequency);
                    }
                }
            }
        }
    }

    /**
     * Close the database connection.
     * 
     * @throws IOException if database cannot be closed
     */
    public void close() throws IOException {
        if (recman != null) {
            recman.close();
        }
    }

    /**
     * Container for keyword extraction results.
     */
    public static class KeywordFrequencyResult {
        public final String[] keywords;
        public final Integer[] frequencies;

        public KeywordFrequencyResult(String[] keywords, Integer[] frequencies) {
            this.keywords = keywords;
            this.frequencies = frequencies;
        }
    }

    /**
     * Example usage: extract keywords from database and create WebpageData.
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: DbKeywordExtractor <dbName> <pageId>");
            System.exit(1);
        }

        String dbName = args[0];
        int pageId = Integer.parseInt(args[1]);

        DbKeywordExtractor extractor = new DbKeywordExtractor(dbName);
        
        try {
            KeywordFrequencyResult result = extractor.extractKeywordsForPage(pageId);
            
            System.out.println("Keywords for page " + pageId + ":");
            for (int i = 0; i < result.keywords.length; i++) {
                System.out.println("  " + result.keywords[i] + ": " + result.frequencies[i]);
            }
        } finally {
            extractor.close();
        }
    }
}

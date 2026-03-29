public class TestDbExtractor {
    public static void main(String[] args) throws Exception {
        System.out.println("Testing DbKeywordExtractor with persisted database...");
        DbKeywordExtractor extractor = new DbKeywordExtractor("indexDB");
        
        // Test extracting keywords for page 1
        DbKeywordExtractor.KeywordFrequencyResult result = extractor.extractKeywordsForPage(1);
        
        System.out.println("\nKeywords for page 1:");
        if (result.keywords.length == 0) {
            System.out.println("  (no keywords found)");
        } else {
            for (int i = 0; i < result.keywords.length; i++) {
                System.out.println("  " + result.keywords[i] + " => " + result.frequencies[i]);
            }
        }
        
        System.out.println("\nTesting page 5...");
        result = extractor.extractKeywordsForPage(5);
        if (result.keywords.length == 0) {
            System.out.println("  (no keywords found)");
        } else {
            for (int i = 0; i < Math.min(5, result.keywords.length); i++) {
                System.out.println("  " + result.keywords[i] + " => " + result.frequencies[i]);
            }
            if (result.keywords.length > 5) {
                System.out.println("  ... and " + (result.keywords.length - 5) + " more");
            }
        }
    }
}

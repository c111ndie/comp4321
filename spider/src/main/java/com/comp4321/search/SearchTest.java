package com.comp4321.search;

import com.comp4321.spider.indexer.StopStem;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.htree.HTree;

import java.io.IOException;
import java.util.*;

/**
 * Test the search engine with an existing JDBM 1.0 database.
 * <p>
 * Assumes the database files are named "indexDB" (from your --db-name argument).
 * The stopwords file is "stopwords.txt" in the current directory.
 */
public class SearchTest {

    public static void main(String[] args) throws IOException {
        // 1. Open the existing JDBM 1.0 database
        String dbName = "indexDB";          // matches --db-name used in indexing
        RecordManager recman = RecordManagerFactory.createRecordManager(dbName);

        // 2. Retrieve the record IDs of the HTrees (they were saved using setNamedObject)
        long wordIdRec = recman.getNamedObject("wordToWordId");
        long bodyIdRec = recman.getNamedObject("bodyInvertedIndex");
        long metaIdRec = recman.getNamedObject("pageMetadata");

        // 3. Load the HTrees using the record IDs
        HTree wordToWordId = HTree.load(recman, wordIdRec);
        HTree bodyInvertedIndex = HTree.load(recman, bodyIdRec);
        HTree pageMetadata = HTree.load(recman, metaIdRec);

        // 4. Create StopStem with the stopwords file (used during indexing)
        String stopwordsFile = "stopwords.txt";   // adjust path if needed
        StopStem stopStem = new StopStem(stopwordsFile);

        // 5. Create a simple query parser (same as before)
        QueryParser parser = new QueryParser(stopStem);

        // 6. Test queries
        String[] testQueries = {
            "news",
            "new",
            "\"hong kong\"",
            "news \"hong kong\""
        };

        for (String queryStr : testQueries) {
            System.out.println("\n=== Query: " + queryStr + " ===");
            Query query = parser.parse(queryStr);
            System.out.println("  Single terms: " + query.getSingleTerms());
            System.out.println("  Phrases: " + query.getPhrases());

            // The Search class constructor expects HTree (JDBM 1.0 works fine)
            Search search = new Search(query, wordToWordId, bodyInvertedIndex, pageMetadata);
            List<Integer> results = search.execute();

            if (results.isEmpty()) {
                System.out.println("  No results.");
            } else {
                System.out.print("  Top document IDs: ");
                results.stream().limit(10).forEach(id -> System.out.print(id + " "));
                System.out.println();
            }
        }

        recman.close();
    }

    /**
     * Simple query parser that handles quoted phrases and single terms,
     * applying stopword removal and stemming.
     */
    static class QueryParser {
        private final StopStem stopStem;

        public QueryParser(StopStem stopStem) {
            this.stopStem = stopStem;
        }

        public Query parse(String rawQuery) {
            List<String> singleTerms = new ArrayList<>();
            List<List<String>> phrases = new ArrayList<>();

            List<String> tokens = tokenizePreserveQuotes(rawQuery);
            for (String token : tokens) {
                if (token.startsWith("\"") && token.endsWith("\"") && token.length() > 1) {
                    // Phrase: remove quotes, split, stop+stem each word
                    String phraseContent = token.substring(1, token.length() - 1);
                    String[] words = phraseContent.split("\\s+");
                    List<String> stemmedPhrase = new ArrayList<>();
                    for (String w : words) {
                        String lower = w.toLowerCase();
                        if (!stopStem.isStopWord(lower)) {
                            String stem = stopStem.stem(lower);
                            if (!stem.isEmpty()) stemmedPhrase.add(stem);
                        }
                    }
                    if (!stemmedPhrase.isEmpty()) phrases.add(stemmedPhrase);
                } else {
                    // Single term
                    String lower = token.toLowerCase();
                    if (!stopStem.isStopWord(lower)) {
                        String stem = stopStem.stem(lower);
                        if (!stem.isEmpty()) singleTerms.add(stem);
                    }
                }
            }
            return new Query(singleTerms, phrases);
        }

        private List<String> tokenizePreserveQuotes(String text) {
            List<String> result = new ArrayList<>();
            boolean inQuotes = false;
            StringBuilder current = new StringBuilder();
            for (char c : text.toCharArray()) {
                if (c == '"') {
                    if (inQuotes) {
                        current.append(c);
                        result.add(current.toString());
                        current.setLength(0);
                        inQuotes = false;
                    } else {
                        if (current.length() > 0) {
                            result.add(current.toString());
                            current.setLength(0);
                        }
                        current.append(c);
                        inQuotes = true;
                    }
                } else if (c == ' ' && !inQuotes) {
                    if (current.length() > 0) {
                        result.add(current.toString());
                        current.setLength(0);
                    }
                } else {
                    current.append(c);
                }
            }
            if (current.length() > 0) {
                result.add(current.toString());
            }
            return result;
        }
    }
}
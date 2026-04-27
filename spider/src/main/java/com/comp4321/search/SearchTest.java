package com.comp4321.search;

import com.comp4321.spider.indexer.PostingList;
import com.comp4321.spider.indexer.PageMetadata;
import com.comp4321.spider.indexer.StopStem;   // your stopword+stemmer class

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.htree.HTree;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Test the search engine against an existing JDBM index.
 * <p>
 * Assumes the database files (*.db) are located in the current working directory
 * or in a known path (e.g., "spider_db"). Adjust DB_PATH if needed.
 */
public class SearchTest {

    private static final String DB_PATH = "indexDB";  // change to your actual DB folder

    public static void main(String[] args) throws IOException {
        // 1. Open the existing JDBM database
        RecordManager recman = RecordManagerFactory.createRecordManager(DB_PATH);
        HTree wordToWordId = HTree.load(recman, "wordToWordId");
        HTree bodyInvertedIndex = HTree.load(recman, "bodyInvertedIndex");
        HTree pageMetadata = HTree.load(recman, "pageMetadata");

        // 2. Create a QueryParser that uses the same StopStem as the indexer
        StopStem stopStem = new StopStem();  // assuming default constructor loads stopwords and Porter stemmer
        QueryParser parser = new QueryParser(stopStem);

        // 3. Example queries (you can change these)
        String[] testQueries = {
            "news",                // single term
            "new",                 // stopword – should return nothing
            "hong kong",           // phrase (no quotes here, but parser will detect as phrase if quoted)
            "\"hong kong\"",       // explicitly quoted phrase
            "news \"hong kong\"",  // AND of term and phrase -> likely empty
            "york"                 // from doc "new york news" -> "york" stemmed to "york"
        };

        for (String queryStr : testQueries) {
            System.out.println("\n=== Query: " + queryStr + " ===");
            Query query = parser.parse(queryStr);
            System.out.println("  Single terms: " + query.getSingleTerms());
            System.out.println("  Phrases: " + query.getPhrases());

            Search search = new Search(query, wordToWordId, bodyInvertedIndex, pageMetadata);
            List<Integer> results = search.execute();

            if (results.isEmpty()) {
                System.out.println("  No results.");
            } else {
                System.out.print("  Top document IDs: ");
                // show first 10 only
                results.stream().limit(10).forEach(id -> System.out.print(id + " "));
                System.out.println();
            }
        }

        recman.close();
    }

    /**
     * Simple query parser that splits a raw query string into single terms and phrases.
     * Phrases are detected by double quotes (e.g., "hong kong").
     * Each term is stopped and stemmed using the given StopStem.
     * <p>
     * Note: For phrase search, the original multi-word string is kept as a list of
     *       words (already stemmed individually) for the Query object. The Search class
     *       will later look up the stemmed words.
     */
    static class QueryParser {
        private final StopStem stopStem;

        public QueryParser(StopStem stopStem) {
            this.stopStem = stopStem;
        }

        public Query parse(String rawQuery) {
            List<String> singleTerms = new ArrayList<>();
            List<List<String>> phrases = new ArrayList<>();

            // Very simple parsing: split by spaces but respect quotes
            List<String> tokens = tokenizePreserveQuotes(rawQuery);
            int i = 0;
            while (i < tokens.size()) {
                String token = tokens.get(i);
                if (token.startsWith("\"") && token.endsWith("\"") && token.length() > 1) {
                    // quoted phrase: remove surrounding quotes, split into words, stop+stem each word
                    String phraseContent = token.substring(1, token.length() - 1);
                    String[] words = phraseContent.split("\\s+");
                    List<String> stemmedPhraseWords = new ArrayList<>();
                    for (String w : words) {
                        String lower = w.toLowerCase();
                        if (!stopStem.isStopWord(lower)) {
                            String stemmed = stopStem.stem(lower);
                            if (!stemmed.isEmpty()) {
                                stemmedPhraseWords.add(stemmed);
                            }
                        }
                    }
                    if (!stemmedPhraseWords.isEmpty()) {
                        phrases.add(stemmedPhraseWords);
                    }
                } else {
                    // single term: stop + stem
                    String lower = token.toLowerCase();
                    if (!stopStem.isStopWord(lower)) {
                        String stemmed = stopStem.stem(lower);
                        if (!stemmed.isEmpty()) {
                            singleTerms.add(stemmed);
                        }
                    }
                }
                i++;
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
                        // closing quote
                        current.append(c);
                        result.add(current.toString());
                        current.setLength(0);
                        inQuotes = false;
                    } else {
                        // opening quote: flush any pending non-quoted token
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
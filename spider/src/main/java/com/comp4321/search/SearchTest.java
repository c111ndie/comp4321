package com.comp4321.search;

import jdbm.helper.FastIterator;
import com.comp4321.spider.indexer.StopStem;
import com.comp4321.spider.indexer.PageMeta;
import com.comp4321.spider.indexer.PostingList;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.htree.HTree;
import java.util.stream.Collectors;

import java.io.*;
import java.util.*;

public class SearchTest {

    public static void main(String[] args) throws IOException {
        String dbName = "indexDB";  
        RecordManager recman = RecordManagerFactory.createRecordManager(dbName);

        Long wordIdRec = (Long) recman.getNamedObject("wordToWordId");
        Long bodyIdRec = (Long) recman.getNamedObject("bodyInvertedIndex");
        Long metaIdRec = (Long) recman.getNamedObject("pageMetadata");

        if (wordIdRec == null || bodyIdRec == null || metaIdRec == null) {
            System.err.println("Named objects not found. Did the indexer store them?");
            System.err.println("Attempting brute-force loading...");
            System.err.println("Unavailable feature. Terminating test.");
            return;
        }

        HTree wordToWordId = HTree.load(recman, wordIdRec);
        HTree bodyInvertedIndex = HTree.load(recman, bodyIdRec);
        HTree pageMetadata = HTree.load(recman, metaIdRec);

        System.out.println("HTrees loaded successfully.");

        // StopStem requires stopwords file
        String stopwordsFile = "stopwords.txt";
        StopStem stopStem;
        stopStem = new StopStem(stopwordsFile);
        QueryParser parser = new QueryParser(stopStem);

        // Printing for debug purposes
        if (false)
        {
            // Diagnostic: test one known word
            System.out.println("\n=== DIAGNOSTIC: search for 'maintain' (exists in page 53) ===");
            Integer wid = (Integer) wordToWordId.get("maintain");
            if (wid != null) {
                PostingList pl = (PostingList) bodyInvertedIndex.get(wid);
                System.out.println("  Posting list page IDs: " + pl.getPageIds());
                // Build a query with this single term and run Search.execute
                List<String> terms = new ArrayList<>();
                terms.add("maintain");
                Query diagQuery = new Query(terms, new ArrayList<>());
                Search diagSearch = new Search(diagQuery, wordToWordId, bodyInvertedIndex, pageMetadata);
                List<Integer> results = diagSearch.execute();
                System.out.println("  Search.execute() returned: " + results);
            } else {
                System.out.println("  'maintain' not found in wordToWordId");
            }
            System.out.println("=== End diagnostic ===\n");
        }
        String[] testQueries = {
            "news",
            "new",
            "computer",
            "maintain",
            "\"hong kong\"",
            "news \"hong kong\""
        };

        for (String queryStr : testQueries) {
            System.out.println("\n=== Query: " + queryStr + " ===");
            Query query = parser.parse(queryStr);
            System.out.println("  Parsed query: single=" + query.getSingleTerms() + ", phrases=" + query.getPhrases());
            System.out.println("  Single terms: " + query.getSingleTerms());
            System.out.println("  Phrases: " + query.getPhrases());

            Search search = new Search(query, wordToWordId, bodyInvertedIndex, pageMetadata);
            List<Integer> results = search.execute();

            if (results.isEmpty()) {
                System.out.println("  No results.");
            } else {
                System.out.print("  Top documents: ");
                for (Search.SearchResult r : results.stream().limit(10).collect(Collectors.toList())) {
                    System.out.print(r.docId + "(score=" + String.format("%.4f", r.score) + ") ");
                }
                System.out.println();
            }
        }

        recman.close();
    }

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
                    String phraseContent = token.substring(1, token.length() - 1);
                    String[] words = phraseContent.split("\\s+");
                    List<String> stemmedPhrase = new ArrayList<>();
                    for (String w : words) {
                        String lower = w.toLowerCase();
                        if (!stopStem.isStopWord(lower)) {
                            if (!lower.isEmpty()) stemmedPhrase.add(lower);  // use original word, not stem
                            //String stem = stopStem.stem(lower);
                            //if (!stem.isEmpty()) stemmedPhrase.add(stem);
                        }
                    }
                    if (!stemmedPhrase.isEmpty()) phrases.add(stemmedPhrase);
                } else {
                    String lower = token.toLowerCase();
                    if (!stopStem.isStopWord(lower)) {
                        if (!lower.isEmpty()) singleTerms.add(lower);  // use original word, not stem
                        //String stem = stopStem.stem(lower);
                        //if (!stem.isEmpty()) singleTerms.add(stem);
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
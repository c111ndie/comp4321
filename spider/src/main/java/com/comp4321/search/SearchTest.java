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
        String dbName = "crawl-output/indexDB";
        RecordManager recman = RecordManagerFactory.createRecordManager(dbName);

        Long wordIdRec = (Long) recman.getNamedObject("wordToWordId");
        Long bodyIdRec = (Long) recman.getNamedObject("bodyInvertedIndex");
        Long metaIdRec = (Long) recman.getNamedObject("pageMetadata");
        Long titleIdRec = (Long) recman.getNamedObject("titleInvertedIndex");

        if (wordIdRec == null || bodyIdRec == null || metaIdRec == null || titleIdRec == null) {
            System.err.println("Named objects not found. Did the indexer store them?");
            System.err.println("Attempting brute-force loading...");
            System.err.println("Unavailable feature. Terminating test.");
            return;
        }

        HTree wordToWordId = HTree.load(recman, wordIdRec);
        HTree bodyInvertedIndex = HTree.load(recman, bodyIdRec);
        HTree pageMetadata = HTree.load(recman, metaIdRec);
        HTree titleInvertedIndex = HTree.load(recman, titleIdRec);

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
                Search diagSearch = new Search(diagQuery, wordToWordId, bodyInvertedIndex, titleInvertedIndex, pageMetadata);
                List<Search.SearchResult> results = diagSearch.execute();
                System.out.println("  Search.execute() returned: " + results.stream().map(r -> r.docId).collect(Collectors.toList()));
            } else {
                System.out.println("  'maintain' not found in wordToWordId");
            }
            System.out.println("=== End diagnostic ===\n");
        }
        String[] testQueries = {
            "the world of computers",
            "computer",
            "hkust",
            "\"hong kong\"",
            "news \"hong kong\"",
            "movi maintain",
            "movi"              
        };

        for (String queryStr : testQueries) {
            System.out.println("\n=== Query: " + queryStr + " ===");
            Query query = parser.parse(queryStr);
            System.out.println("  Parsed query: single=" + query.getSingleTerms() + ", phrases=" + query.getPhrases());
            System.out.println("  Single terms: " + query.getSingleTerms());
            System.out.println("  Phrases: " + query.getPhrases());

            Search search = new Search(query, wordToWordId, bodyInvertedIndex, titleInvertedIndex, pageMetadata);

            List<Search.SearchResult> results = search.execute();
            if (results.isEmpty()) {
                System.out.println("  No results.");
            } else {
                System.out.print("  Top documents: ");
                for (Search.SearchResult r : results.stream().limit(10).collect(Collectors.toList())) {
                    System.out.print(r.docId + "(score=" + String.format("%.4f", r.score) + ", \nmissing=" + r.missingKeywords + ") ");
                }
                System.out.println();
            }
        }

        recman.close();
    }
}
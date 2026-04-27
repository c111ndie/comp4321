package com.comp4321.spider.indexer;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.helper.FastIterator;
import jdbm.htree.HTree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Search {
    private Query query;   // <-- stored query structure
    private HTree wordToWordId;
    private HTree bodyInvertedIndex;
    private HTree pageMetadata;

    public Search(Query query, HTree wordToWordId, HTree bodyInvertedIndex, HTree pageMetadata) {
        this.query = query;
        this.wordToWordId = wordToWordId;
        this.bodyInvertedIndex = bodyInvertedIndex;
        this.pageMetadata = pageMetadata;
    }
        
    public void setQuery(Query query) {
        this.query = query;
    }

    public List<Integer> execute() throws IOException {
        List<Integer> result = null;

        // Process single terms
        for (String term : query.getSingleTerms()) {
            List<Integer> docs = searchTerm(term);
            if (result == null) result = docs;
            else result.retainAll(docs);   // AND
        }

        // Process phrases
        for (List<String> phraseWords : query.getPhrases()) {
            List<Integer> docs = searchPhrase(phraseWords);
            if (result == null) result = docs;
            else result.retainAll(docs);
        }

        return result == null ? Collections.emptyList() : result;
    }

    private List<Integer> searchTerm(String stem) throws IOException {
        Integer wordId = (Integer) wordToWordId.get(stem);
        if (wordId == null) return Collections.emptyList();
        PostingList pl = (PostingList) bodyInvertedIndex.get(wordId);
        return new ArrayList<>(pl.getDocuments());
    }


    /**
     * Searches for an exact phrase in the body text of indexed pages.
     * <p>
     * The phrase is split into words, each word is looked up in the dictionary,
     * and the positional inverted index is used to find documents where the words
     * appear consecutively in the given order.
     * <p>
     * Example: phrase "hong kong" matches documents containing "hong" immediately
     * followed by "kong" with no intervening tokens.
     *
     * @param phrase the quoted query string without the quotes (e.g., "hong kong")
     * @return a list of page IDs that contain the exact phrase, ordered by
     *         document ID (no ranking applied yet). Empty list if no match or
     *         any word in the phrase is not in the dictionary.
     * @throws IOException if accessing JDBM indexes fails
     *
     * @see #bodyInvertedIndex
     * @see #wordToWordId
     */
    public List<Integer> searchPhrase(String phrase) throws IOException {
        String[] words = phrase.toLowerCase().split("\\s+");
        List<Integer> wordIds = new ArrayList<>();
        for (String w : words) {
            Integer wid = (Integer) wordToWordId.get(w);
            if (wid == null) return Collections.emptyList(); // unknown word
            wordIds.add(wid);
        }
        
        // Get posting lists for each word
        List<PostingList> plists = new ArrayList<>();
        for (int wid : wordIds) {
            PostingList pl = (PostingList) bodyInvertedIndex.get(wid);
            if (pl == null) return Collections.emptyList();
            plists.add(pl);
        }
        
        // Start with documents from the first term's posting list
        List<Integer> result = new ArrayList<>();
        PostingList first = plists.get(0);
        for (int docId : first.getDocuments()) {
            boolean match = true;
            // For each subsequent term, check positions
            for (int i = 1; i < wordIds.size(); i++) {
                PostingList current = plists.get(i);
                if (!current.containsDocument(docId)) {
                    match = false;
                    break;
                }
                // Check adjacency: positions[i] should equal positions[0] + i
                if (!hasAdjacentPositions(first.getPositions(docId),
                                        current.getPositions(docId), i)) {
                    match = false;
                    break;
                }
            }
            if (match) result.add(docId);
        }
        return result;
    }

    private boolean hasAdjacentPositions(List<Integer> posFirst,
                                        List<Integer> posOther, int offset) {
        // For each start position in first list, see if posFirst + offset exists in posOther
        for (int p : posFirst) {
            if (Collections.binarySearch(posOther, p + offset) >= 0)
                return true;
        }
        return false;
    }
}
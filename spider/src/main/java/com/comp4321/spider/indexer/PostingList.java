package com.comp4321.spider.indexer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stores a posting list for one term: maps integer pageId to the list of
 * positions (0-based token offsets) where the term appears in that page.
 * Serializable so it can be stored directly in a JDBM HTree.
 */
public class PostingList implements Serializable {
    private static final long serialVersionUID = 2L;

    // pageId -> ordered list of token positions
    private final Map<Integer, List<Integer>> postings = new HashMap<>();

    public void addOccurrence(int pageId, int position) {
        postings.computeIfAbsent(pageId, k -> new ArrayList<>()).add(position);
    }

    public List<Integer> getPositions(int pageId) {
        return postings.getOrDefault(pageId, Collections.emptyList());
    }

    public Set<Integer> getPageIds() {
        return postings.keySet();
    }

    /** Number of documents containing the term. */
    public int getDocumentFrequency() {
        return postings.size();
    }

    /** Number of times the term appears in the given page. */
    public int getTermFrequency(int pageId) {
        List<Integer> pos = postings.get(pageId);
        return pos == null ? 0 : pos.size();
    }

    /** Full postings map: pageId -> positions list. */
    public Map<Integer, List<Integer>> getPostings() {
        return postings;
    }
}

package com.comp4321.search;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a search query parsed into single terms and exact phrases.
 * Example: "apple pie" (phrase) banana (term) -> 
 *   phrases = [["apple", "pie"]]
 *   singleTerms = ["banana"]
 */
public class Query {
    private final List<String> singleTerms = new ArrayList<>();
    private final List<List<String>> phrases = new ArrayList<>();

    /**
     * Default constructor for an empty query.
     */
    public Query() {
    }

    /**
     * Constructor that initializes the query with terms and phrases.
     *
     * @param singleTerms list of single search terms
     * @param phrases     list of exact phrases (each phrase is a list of consecutive words)
     */
    public Query(List<String> singleTerms, List<List<String>> phrases) {
        if (singleTerms != null) {
            this.singleTerms.addAll(singleTerms);
        }
        if (phrases != null) {
            for (List<String> phrase : phrases) {
                this.phrases.add(new ArrayList<>(phrase));
            }
        }
    }

    /**
     * Adds a single search term to this query.
     */
    public void addTerm(String term) {
        singleTerms.add(term);
    }

    /**
     * Adds an exact phrase (multiple consecutive words) to this query.
     */
    public void addPhrase(List<String> phraseWords) {
        phrases.add(new ArrayList<>(phraseWords));
    }

    /**
     * Returns all single (non-phrase) terms in this query.
     */
    public List<String> getSingleTerms() {
        return singleTerms;
    }

    /**
     * Returns all exact phrases in this query.
     * Each phrase is a list of consecutive words.
     */
    public List<List<String>> getPhrases() {
        return phrases;
    }

    /**
     * Returns true if this query has any terms or phrases.
     */
    public boolean isEmpty() {
        return singleTerms.isEmpty() && phrases.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Query{");
        if (!singleTerms.isEmpty()) {
            sb.append("terms=").append(singleTerms);
        }
        if (!phrases.isEmpty()) {
            if (!singleTerms.isEmpty()) sb.append(", ");
            sb.append("phrases=").append(phrases);
        }
        sb.append("}");
        return sb.toString();
    }
}

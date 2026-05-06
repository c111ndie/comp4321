package com.comp4321.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a search query parsed into single terms and exact phrases.
 * Example: "apple pie" (phrase) banana (term) -> 
 *   phrases = [["apple", "pie"]]
 *   singleTerms = ["banana"]
 * 
 * Immutable and serializable for potential persistence.
 */
public class Query implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final List<String> singleTerms;      // e.g., ["restaurant"]
    private final List<List<String>> phrases;    // e.g., [ ["hong","kong"] ]

    /**
     * Default constructor for an empty query.
     */
    public Query() {
        this.singleTerms = new ArrayList<>();
        this.phrases = new ArrayList<>();
    }

    /**
     * Constructor that initializes the query with terms and phrases.
     *
     * @param singleTerms list of single search terms
     * @param phrases     list of exact phrases (each phrase is a list of consecutive words)
     */
    public Query(List<String> singleTerms, List<List<String>> phrases) {
        this.singleTerms = singleTerms;
        this.phrases = phrases;
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

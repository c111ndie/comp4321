import java.io.Serializable;
import java.util.List;

/**
 * Represents a parsed query that can be stored and executed later.
 * Immutable and serializable for potential persistence.
 */
public class Query implements Serializable {
    private final List<String> singleTerms;      // e.g., ["restaurant"]
    private final List<List<String>> phrases;    // e.g., [ ["hong","kong"] ]

    public Query(List<String> singleTerms, List<List<String>> phrases) {
        this.singleTerms = singleTerms;
        this.phrases = phrases;
    }

    public List<String> getSingleTerms() {
        return singleTerms;
    }

    public List<List<String>> getPhrases() {
        return phrases;
    }
}
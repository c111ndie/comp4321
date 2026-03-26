import java.io.Serializable;
import java.util.*;

public class PostingList implements Serializable {
    private static final long serialVersionUID = 1L;
    private Map<String, Integer> postings; // docId -> tf

    public PostingList() {
        postings = new HashMap<>();
    }

    public void addOccurrence(String docId) {
        postings.put(docId, postings.getOrDefault(docId, 0) + 1);
    }

    public int getTermFrequency(String docId) {
        return postings.getOrDefault(docId, 0);
    }

    public int getDocumentFrequency() {
        return postings.size();
    }

    public Map<String, Integer> getPostings() {
        return postings;
    }
}
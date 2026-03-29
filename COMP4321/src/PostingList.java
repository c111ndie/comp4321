import java.io.Serializable;
import java.util.*;

public class PostingList implements Serializable {
    private static final long serialVersionUID = 1L;
    private Map<String, List<Integer>> postings; // docId -> tf

    public PostingList() {
        postings = new HashMap<>();
    }

    public void addOccurrence(String docId, int position) {
        postings.computeIfAbsent(docId, k -> new ArrayList<>()).add(position);
    }

    public List<Integer> getPositions(String docId) {
        return postings.getOrDefault(docId, Collections.emptyList());
    }

    /*public int getTermFrequency(String docId) {
        return postings.getOrDefault(docId, 0);
    }*/
    
    public Set<String> getDocuments() {
        return postings.keySet();
    }

    public int getDocumentFrequency() {
        return postings.size();
    }

    public Map<String, Integer> getPostings() {
        return postings;
    }
}

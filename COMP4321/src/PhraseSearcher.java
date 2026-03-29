import java.io.IOException;
import java.util.*;

public class PhraseSearcher {
    private InvertedIndex index;

    public PhraseSearcher(InvertedIndex index) {
        this.index = index;
    }

    /**
     * Finds all documents containing the exact phrase (e.g. "hong kong").
     */
    public Set<String> searchPhrase(String phrase, boolean inTitle) throws IOException {
        String[] words = phrase.toLowerCase().split("\\W+");
        if (words.length == 0) return Collections.emptySet();

        // Get posting lists for all words
        List<Map<String, List<Integer>>> postingLists = new ArrayList<>();
        for (String w : words) {
            PostingList plist = (PostingList) (inTitle ? index.getTitleTerm(w) : index.getBodyTerm(w));
            if (plist == null) return Collections.emptySet();
            postingLists.add(plist.getPostings());
        }

        // Start with set of docs containing the first word
        Set<String> candidateDocs = new HashSet<>(postingLists.get(0).keySet());

        // Intersect with other word docs
        for (int i = 1; i < postingLists.size(); i++) {
            candidateDocs.retainAll(postingLists.get(i).keySet());
        }

        // Keep only docs where word positions appear consecutively
        Set<String> phraseDocs = new HashSet<>();
        for (String docId : candidateDocs) {
            boolean match = true;
            List<Integer> prevPositions = postingLists.get(0).get(docId);

            for (int i = 1; i < postingLists.size(); i++) {
                List<Integer> nextPositions = postingLists.get(i).get(docId);
                boolean found = false;
                for (int pos : prevPositions) {
                    if (nextPositions.contains(pos + i)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    match = false;
                    break;
                }
            }
            if (match) phraseDocs.add(docId);
        }

        return phraseDocs;
    }
}

package com.comp4321.spider.indexer;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.helper.FastIterator;
import jdbm.htree.HTree;

import java.io.IOException;
import java.util.*;

public class Search_rank {
    private Query query;
    private HTree wordToWordId;         // stem -> wordId
    private HTree bodyInvertedIndex;    // wordId -> PostingList
    private HTree pageMetadata;         // docId -> PageMetadata object

    // Total number of documents (stored in pageMetadata under a special key, e.g. -1)
    private int totalDocuments;

    public Search_rank(Query query, HTree wordToWordId, HTree bodyInvertedIndex, 
                  HTree pageMetadata) throws IOException {
        this.query = query;
        this.wordToWordId = wordToWordId;
        this.bodyInvertedIndex = bodyInvertedIndex;
        this.pageMetadata = pageMetadata;
        // Assume the total number of documents is stored under key -1.
        // If not, compute by iterating over pageMetadata keys (excluding -1).
        Integer storedTotal = (Integer) pageMetadata.get(-1);
        if (storedTotal != null) {
            this.totalDocuments = storedTotal;
        } else {
            // Fallback: count document IDs from pageMetadata
            int count = 0;
            FastIterator keys = pageMetadata.keys();
            Object key;
            while ((key = keys.next()) != null) {
                if (!key.equals(-1)) count++;
            }
            this.totalDocuments = count;
        }
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    /**
     * Executes the query with AND semantics, computes cosine similarity for each
     * matching document, and returns document IDs sorted by descending score.
     *
     * @return list of page IDs (documents) in descending order of relevance.
     */
    public List<Integer> execute() throws IOException {
        // 1. Gather all query components (single terms and phrases) and compute
        //    their posting lists and IDF values.
        List<QueryComponent> components = new ArrayList<>();

        // Process single terms
        for (String term : query.getSingleTerms()) {
            Integer wordId = (Integer) wordToWordId.get(term);
            if (wordId == null) continue; // term not in index -> no document can match
            PostingList pl = (PostingList) bodyInvertedIndex.get(wordId);
            if (pl == null) continue;
            double idf = computeIdf(pl.getDocumentFrequency()); // df = number of docs containing term
            components.add(new TermComponent(term, pl, idf));
        }

        // Process phrases
        for (List<String> phraseWords : query.getPhrases()) {
            // Compute the posting list and phrase frequency for the exact phrase
            PhraseData phraseData = computePhraseData(phraseWords);
            if (phraseData == null) continue; // unknown word or no documents
            double idf = computeIdf(phraseData.postingList.getDocumentFrequency());
            components.add(new PhraseComponent(phraseWords, phraseData, idf));
        }

        if (components.isEmpty()) return Collections.emptyList();

        // 2. Intersect document sets from all components (AND semantics)
        Set<Integer> candidates = null;
        for (QueryComponent comp : components) {
            Set<Integer> docSet = comp.getDocumentSet();
            if (candidates == null) candidates = new HashSet<>(docSet);
            else candidates.retainAll(docSet);
        }
        if (candidates == null || candidates.isEmpty()) return Collections.emptyList();

        // 3. For each candidate document, compute cosine similarity
        Map<Integer, Double> scores = new HashMap<>();
        for (int docId : candidates) {
            double dotProduct = 0.0;
            for (QueryComponent comp : components) {
                double docWeight = comp.getDocumentWeight(docId);
                double queryWeight = comp.getQueryWeight();
                dotProduct += docWeight * queryWeight;
            }
            Double docNorm = getDocumentNorm(docId);
            if (docNorm == null || docNorm == 0.0) continue;
            double queryNorm = computeQueryNorm(components);
            double cosine = dotProduct / (docNorm * queryNorm);
            scores.put(docId, cosine);
        }

        // 4. Sort documents by descending score
        List<Integer> result = new ArrayList<>(scores.keySet());
        result.sort((a, b) -> Double.compare(scores.get(b), scores.get(a)));
        return result;
    }

    // ---------- Helper methods for IDF, norms, phrase processing ----------

    private double computeIdf(int documentFrequency) {
        if (documentFrequency == 0) return 0.0;
        return Math.log((double) totalDocuments / documentFrequency);
    }

    private Double getDocumentNorm(int docId) throws IOException {
        PageMetadata meta = (PageMetadata) pageMetadata.get(docId);
        return meta == null ? null : meta.getNorm();
    }

    private Double getMaxTermFrequency(int docId) throws IOException {
        PageMetadata meta = (PageMetadata) pageMetadata.get(docId);
        return meta == null ? null : meta.getMaxTermFrequency();
    }

    private double computeQueryNorm(List<QueryComponent> components) {
        double sumSq = 0.0;
        for (QueryComponent comp : components) {
            double qw = comp.getQueryWeight();
            sumSq += qw * qw;
        }
        return Math.sqrt(sumSq);
    }

    /**
     * Computes posting list and phrase frequencies for an exact phrase.
     * Returns null if any word in the phrase is missing from the dictionary.
     */
    private PhraseData computePhraseData(List<String> phraseWords) throws IOException {
        // Map each word to its wordId and posting list
        List<Integer> wordIds = new ArrayList<>();
        List<PostingList> postingLists = new ArrayList<>();
        for (String word : phraseWords) {
            Integer wid = (Integer) wordToWordId.get(word);
            if (wid == null) return null;
            PostingList pl = (PostingList) bodyInvertedIndex.get(wid);
            if (pl == null) return null;
            wordIds.add(wid);
            postingLists.add(pl);
        }

        // Intersect documents that contain all words (necessary for phrase)
        Set<Integer> docsWithAllWords = new HashSet<>(postingLists.get(0).getDocuments());
        for (int i = 1; i < postingLists.size(); i++) {
            docsWithAllWords.retainAll(postingLists.get(i).getDocuments());
        }

        // For each document, compute phrase frequency (consecutive occurrences)
        Map<Integer, Integer> phraseFreq = new HashMap<>();
        for (int docId : docsWithAllWords) {
            List<Integer> firstPositions = postingLists.get(0).getPositions(docId);
            int count = 0;
            // For each occurrence of the first word, check if the next words follow immediately
            for (int pos : firstPositions) {
                boolean fullMatch = true;
                for (int i = 1; i < postingLists.size(); i++) {
                    List<Integer> positions = postingLists.get(i).getPositions(docId);
                    if (Collections.binarySearch(positions, pos + i) < 0) {
                        fullMatch = false;
                        break;
                    }
                }
                if (fullMatch) count++;
            }
            if (count > 0) {
                phraseFreq.put(docId, count);
            }
        }

        // Build a custom PostingList-like object for the phrase
        List<Integer> phraseDocs = new ArrayList<>(phraseFreq.keySet());
        PhrasePostingList phrasePL = new PhrasePostingList(phraseDocs, phraseFreq);
        return new PhraseData(phrasePL, phraseFreq);
    }

    // ---------- Inner classes to represent query components ----------

    private interface QueryComponent {
        Set<Integer> getDocumentSet();
        double getDocumentWeight(int docId) throws IOException;
        double getQueryWeight();
    }

    private class TermComponent implements QueryComponent {
        private final String term;
        private final PostingList postingList;
        private final double idf;
        private final Set<Integer> docSet;

        TermComponent(String term, PostingList pl, double idf) {
            this.term = term;
            this.postingList = pl;
            this.idf = idf;
            this.docSet = new HashSet<>(pl.getDocuments());
        }

        @Override
        public Set<Integer> getDocumentSet() {
            return docSet;
        }

        @Override
        public double getDocumentWeight(int docId) throws IOException {
            if (!postingList.containsDocument(docId)) return 0.0;
            int tf = postingList.getFrequency(docId); // or postingList.getPositions(docId).size()
            Double maxTf = getMaxTermFrequency(docId);
            if (maxTf == null || maxTf == 0) return 0.0;
            return (tf / maxTf) * idf;
        }

        @Override
        public double getQueryWeight() {
            // query term appears once, max tf in query = 1
            return idf;
        }
    }

    private class PhraseComponent implements QueryComponent {
        private final List<String> words;
        private final PhraseData phraseData;
        private final double idf;
        private final Set<Integer> docSet;

        PhraseComponent(List<String> words, PhraseData data, double idf) {
            this.words = words;
            this.phraseData = data;
            this.idf = idf;
            this.docSet = new HashSet<>(data.postingList.getDocuments());
        }

        @Override
        public Set<Integer> getDocumentSet() {
            return docSet;
        }

        @Override
        public double getDocumentWeight(int docId) throws IOException {
            Integer phraseFreq = phraseData.phraseFrequency.get(docId);
            if (phraseFreq == null) return 0.0;
            Double maxTf = getMaxTermFrequency(docId);
            if (maxTf == null || maxTf == 0) return 0.0;
            return (phraseFreq / maxTf) * idf;
        }

        @Override
        public double getQueryWeight() {
            // phrase appears once in the query
            return idf;
        }
    }

    // Simple data container for phrase results
    private static class PhraseData {
        final PhrasePostingList postingList;
        final Map<Integer, Integer> phraseFrequency;  // docId -> occurrence count
        PhraseData(PhrasePostingList pl, Map<Integer, Integer> freq) {
            this.postingList = pl;
            this.phraseFrequency = freq;
        }
    }

    /**
     * Minimal PostingList implementation for a phrase (not stored in JDBM,
     * only used temporarily during query processing).
     */
    private static class PhrasePostingList {
        private final List<Integer> documents;
        private final Map<Integer, Integer> frequency; // docId -> phrase frequency
        PhrasePostingList(List<Integer> docs, Map<Integer, Integer> freq) {
            this.documents = docs;
            this.frequency = freq;
        }
        List<Integer> getDocuments() { return documents; }
        int getDocumentFrequency() { return documents.size(); }
        int getFrequency(int docId) { return frequency.getOrDefault(docId, 0); }
        boolean containsDocument(int docId) { return frequency.containsKey(docId); }
    }
}
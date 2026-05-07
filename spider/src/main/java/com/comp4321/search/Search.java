package com.comp4321.search;

import com.comp4321.spider.indexer.StopStem;
import com.comp4321.spider.indexer.PostingList;
import com.comp4321.spider.indexer.PageMeta;

import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.helper.FastIterator;
import jdbm.htree.HTree;

import java.io.IOException;
import java.util.*;

public class Search {


    private Query query;
    private HTree wordToWordId;         // stem -> wordId
    private HTree bodyInvertedIndex;    // wordId -> PostingList
    private HTree titleInvertedIndex;   // 
    private HTree pageMeta;         // docId -> PageMeta object
    private double TITLE_BOOST; // boost factor for title matches

    // Total number of documents (stored in pageMeta under a special key, e.g. -1)
    private int totalDocuments;

    public Search(Query query, HTree wordToWordId, HTree bodyInvertedIndex, 
                  HTree pageMeta) throws IOException {
        this.query = query;
        this.wordToWordId = wordToWordId;
        this.bodyInvertedIndex = bodyInvertedIndex;
        this.pageMeta = pageMeta;
        this.TITLE_BOOST = 1.0; // default boost factor for title matches
        // Assume the total number of documents is stored under key -1.
        // If not, compute by iterating over pageMeta keys (excluding -1).
        Integer storedTotal = (Integer) pageMeta.get(-1);
        if (storedTotal != null) {
            this.totalDocuments = storedTotal;
        } 
        else {
            // Fallback: count document IDs from pageMeta
            int count = 0;
            FastIterator keys = pageMeta.keys();
            Object key;
            while ((key = keys.next()) != null) {
                if (!key.equals(-1)) count++;
            }
            this.totalDocuments = count;
        }
    }
    public Search(Query query, HTree wordToWordId, HTree bodyInvertedIndex, HTree titleInvertedIndex, 
                  HTree pageMeta) throws IOException {
        this.query = query;
        this.wordToWordId = wordToWordId;
        this.bodyInvertedIndex = bodyInvertedIndex;
        this.titleInvertedIndex = titleInvertedIndex;
        this.pageMeta = pageMeta;
        this.TITLE_BOOST = 10000;
        // Assume the total number of documents is stored under key -1.
        // If not, compute by iterating over pageMeta keys (excluding -1).
        Integer storedTotal = (Integer) pageMeta.get(-1);
        if (storedTotal != null) {
            this.totalDocuments = storedTotal;
        } else {
            // Fallback: count document IDs from pageMeta
            int count = 0;
            FastIterator keys = pageMeta.keys();
            Object key;
            while ((key = keys.next()) != null) {
                if (!key.equals(-1)) count++;
            }
            this.totalDocuments = count;
        }
    }
    public Search(Query query, HTree wordToWordId, HTree bodyInvertedIndex, HTree titleInvertedIndex, 
                  HTree pageMeta, double titleBoost) throws IOException {
        this.query = query;
        this.wordToWordId = wordToWordId;
        this.bodyInvertedIndex = bodyInvertedIndex;
        this.titleInvertedIndex = titleInvertedIndex;
        this.pageMeta = pageMeta;
        this.TITLE_BOOST = titleBoost;
        // Assume the total number of documents is stored under key -1.
        // If not, compute by iterating over pageMeta keys (excluding -1).
        Integer storedTotal = (Integer) pageMeta.get(-1);
        if (storedTotal != null) {
            this.totalDocuments = storedTotal;
        } else {
            // Fallback: count document IDs from pageMeta
            int count = 0;
            FastIterator keys = pageMeta.keys();
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
    public List<SearchResult> execute() throws IOException {
        // 1. Gather all query components (single terms and phrases) and compute
        //    their posting lists and IDF values.
        List<QueryComponent> components = new ArrayList<>();

        // Process single terms
        for (String term : query.getSingleTerms()) {
            Integer wordId = (Integer) wordToWordId.get(term);
            if (wordId == null) continue; // term not in index -> no document can match
            PostingList bodyPl = (PostingList) bodyInvertedIndex.get(wordId);
            PostingList titlePl = (PostingList) titleInvertedIndex.get(wordId);
            if (bodyPl == null && titlePl == null) continue;
            if (bodyPl == null) bodyPl = EMPTY_POSTING_LIST;
            if (titlePl == null) titlePl = EMPTY_POSTING_LIST;

            // Compute IDF from the union of documents (optional) or from body only
            Set<Integer> allDocs = new TreeSet<>(bodyPl.getPageIds());
            allDocs.addAll(titlePl.getPageIds());
            int df = allDocs.size();                 // document frequency
            double idf = computeIdf(df);             // or computeIdf(bodyPl.getDocumentFrequency()) if you prefer

            components.add(new TermComponent(term, bodyPl, titlePl, idf, this.TITLE_BOOST));
        }

        // Process phrases
        for (List<String> phraseWords : query.getPhrases()) {
            PhraseData phraseData = computePhraseData(phraseWords);
            if (phraseData == null) continue; // unknown word or no documents
            // Compute IDF based on union of documents containing the phrase in body or title
            Set<Integer> allDocs = new TreeSet<>(phraseData.bodyPostingList.getPageIds());
            allDocs.addAll(phraseData.titlePostingList.getPageIds());
            int df = allDocs.size();
            double idf = computeIdf(df);
            components.add(new PhraseComponent(phraseWords, phraseData, idf, TITLE_BOOST));
        }

        if (components.isEmpty()) return Collections.emptyList();

        // 2. Intersect document sets from all components (OR semantics)
        Set<Integer> candidates = new TreeSet<>();
        for (QueryComponent comp : components) {
            candidates.addAll(comp.getDocumentSet());
        }
        if (candidates.isEmpty()) return Collections.emptyList();

        // 3. For each candidate document, compute cosine similarity
        Map<Integer, Double> scores = new LinkedHashMap<>();
        Map<Integer, List<String>> missingTermsMap = new LinkedHashMap<>();
        double queryNorm = computeQueryNorm(components);

        for (int docId : candidates) {
            double dotProduct = 0.0;
            List<String> missing = new ArrayList<>();
            
            for (QueryComponent comp : components) {
                double docWeight = comp.getDocumentWeight(docId);
                double queryWeight = comp.getQueryWeight();
                dotProduct += docWeight * queryWeight;
                
                // Check if this component is absent
                if (!comp.getDocumentSet().contains(docId)) {
                    // comp.getTermString() – you need to add a method to get the term/phrase text
                    missing.add(comp.getDisplayText());
                }
            }
            
            Double docNorm = getDocumentNorm(docId);
            if (docNorm == null || docNorm == 0.0) continue;
            double cosine = dotProduct / (docNorm * queryNorm);
            
            missingTermsMap.put(docId, missing);
            scores.put(docId, cosine);
        }
        // 4. Sort documents by descending score
        List<SearchResult> result = new ArrayList<>();
        for (Map.Entry<Integer, Double> entry : scores.entrySet()) {
            int docId = entry.getKey();
            double score = entry.getValue();
            List<String> missing = missingTermsMap.getOrDefault(docId, Collections.emptyList());
            result.add(new SearchResult(docId, score, missing));
        }
        result.sort(null); // uses SearchResult's compareTo
        return result;
    }

    // ---------- Helper methods for IDF, norms, phrase processing ----------

    private double computeIdf(int documentFrequency) {
        if (documentFrequency == 0) return 0.0;
        return Math.log((double) totalDocuments / documentFrequency);
    }

    private Double getDocumentNorm(int docId) throws IOException {
        PageMeta meta = (PageMeta) pageMeta.get(docId);
        if (meta == null) return 1.0;
        double norm = meta.getNorm();
        return norm == 0 ? 1.0 : norm;
    }

    private Double getMaxTermFrequency(int docId) throws IOException {
        PageMeta meta = (PageMeta) pageMeta.get(docId);
        if (meta == null) return 1.0;
        double maxTf = meta.getMaxTermFrequency();
        return maxTf == 0 ? 1.0 : maxTf;
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
        // Map each word to its wordId and posting lists (both body and title)
        List<Integer> wordIds = new ArrayList<>();
        List<PostingList> bodyPostingLists = new ArrayList<>();
        List<PostingList> titlePostingLists = new ArrayList<>();
        for (String word : phraseWords) {
            Integer wid = (Integer) wordToWordId.get(word);
            if (wid == null) return null;
            PostingList bodyPl = (PostingList) bodyInvertedIndex.get(wid);
            PostingList titlePl = (PostingList) titleInvertedIndex.get(wid);
            if (bodyPl == null && titlePl == null) return null;
            wordIds.add(wid);
            bodyPostingLists.add(bodyPl != null ? bodyPl : EMPTY_POSTING_LIST);
            titlePostingLists.add(titlePl != null ? titlePl : EMPTY_POSTING_LIST);
        }

        // Compute for body
        Map<Integer, Integer> bodyFreq = computePhraseFreqForField(bodyPostingLists);
        // Compute for title
        Map<Integer, Integer> titleFreq = computePhraseFreqForField(titlePostingLists);
        
        // Build PhrasePostingList objects
        List<Integer> bodyDocs = new ArrayList<>(bodyFreq.keySet());
        PhrasePostingList bodyPhrasePl = new PhrasePostingList(bodyDocs, bodyFreq);
        List<Integer> titleDocs = new ArrayList<>(titleFreq.keySet());
        PhrasePostingList titlePhrasePl = new PhrasePostingList(titleDocs, titleFreq);
        
        return new PhraseData(bodyPhrasePl, bodyFreq, titlePhrasePl, titleFreq);
    }

    private Map<Integer, Integer> computePhraseFreqForField(List<PostingList> postingLists) {
        // Intersect documents containing all words
        Set<Integer> docsWithAll = new TreeSet<>(postingLists.get(0).getPageIds());
        for (int i = 1; i < postingLists.size(); i++) {
            docsWithAll.retainAll(postingLists.get(i).getPageIds());
        }
        Map<Integer, Integer> phraseFreq = new LinkedHashMap<>();
        for (int docId : docsWithAll) {
            List<Integer> firstPositions = postingLists.get(0).getPositions(docId);
            int count = 0;
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
            if (count > 0) phraseFreq.put(docId, count);
        }
        return phraseFreq;
    }

    // ---------- Inner classes to represent query components ----------

    private interface QueryComponent {
        Set<Integer> getDocumentSet();
        double getDocumentWeight(int docId) throws IOException;
        double getQueryWeight();
        String getDisplayText();   // new method
    }

    private class TermComponent implements QueryComponent {
        private final String term;
        private final PostingList bodyPl;
        private final PostingList titlePl;
        private final double idf;
        private final Set<Integer> docSet;
        private final double TITLE_BOOST;

        TermComponent(String term, PostingList bodyPl, PostingList titlePl, double idf, double TITLE_BOOST) {
            this.term = term;
            this.bodyPl = bodyPl;
            this.titlePl = titlePl;
            this.idf = idf;
            this.TITLE_BOOST = TITLE_BOOST;
            // Union of documents that contain the term in body OR title
            Set<Integer> docs = new TreeSet<>(bodyPl.getPageIds());
            docs.addAll(titlePl.getPageIds());
            this.docSet = docs;
        }

        @Override
        public Set<Integer> getDocumentSet() { return docSet; }

        @Override
        public double getDocumentWeight(int docId) throws IOException {
            int bodyTf = bodyPl.getTermFrequency(docId);
            int titleTf = titlePl.getTermFrequency(docId);
            if (bodyTf == 0 && titleTf == 0) return 0.0;
            
            Double maxTf = getMaxTermFrequency(docId);
            if (maxTf == null || maxTf == 0) return 0.0;
            
            double bodyWeight = (bodyTf / maxTf) * idf;
            double titleWeight = TITLE_BOOST * (titleTf / maxTf) * idf;
            return bodyWeight + titleWeight;
        }

        @Override
        public double getQueryWeight() { return idf; }
        @Override
        public String getDisplayText() {
            return term;   // the stemmed/original term
        }
    }

    private class PhraseComponent implements QueryComponent {
        private final List<String> words;
        private final PhraseData phraseData;
        private final double idf;
        private final Set<Integer> docSet;
        private final double TITLE_BOOST;

        PhraseComponent(List<String> words, PhraseData data, double idf, double titleBoost) {
            this.words = words;
            this.phraseData = data;
            this.idf = idf;
            this.TITLE_BOOST = titleBoost;
            Set<Integer> allDocs = new TreeSet<>(data.bodyPostingList.getPageIds());
            allDocs.addAll(data.titlePostingList.getPageIds());
            this.docSet = allDocs;
        }

        @Override
        public Set<Integer> getDocumentSet() { 
            Set<Integer> all = new TreeSet<>(phraseData.bodyPostingList.getPageIds());
            all.addAll(phraseData.titlePostingList.getPageIds());
            return all;
        }

        @Override
        public double getDocumentWeight(int docId) throws IOException {
            int bodyFreq = phraseData.bodyFrequency.getOrDefault(docId, 0);
            int titleFreq = phraseData.titleFrequency.getOrDefault(docId, 0);
            if (bodyFreq == 0 && titleFreq == 0) return 0.0;
            Double maxTf = getMaxTermFrequency(docId);
            if (maxTf == null || maxTf == 0) return 0.0;
            double bodyWeight = (bodyFreq / maxTf) * idf;
            double titleWeight = TITLE_BOOST * (titleFreq / maxTf) * idf;
            return bodyWeight + titleWeight;
        }

        @Override
        public double getQueryWeight() { return idf; }

        @Override
        public String getDisplayText() { return String.join(" ", words); }
    }

    // Simple data container for phrase results
    private static class PhraseData {
        final PhrasePostingList bodyPostingList;
        final PhrasePostingList titlePostingList;
        final Map<Integer, Integer> bodyFrequency;
        final Map<Integer, Integer> titleFrequency;
        
        PhraseData(PhrasePostingList bodyPl, Map<Integer, Integer> bodyFreq,
                PhrasePostingList titlePl, Map<Integer, Integer> titleFreq) {
            this.bodyPostingList = bodyPl;
            this.titlePostingList = titlePl;
            this.bodyFrequency = bodyFreq;
            this.titleFrequency = titleFreq;
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
        Set<Integer> getPageIds() { return new TreeSet<>(documents); }
        int getDocumentFrequency() { return documents.size(); }
        int getFrequency(int docId) { return frequency.getOrDefault(docId, 0); }
        boolean containsDocument(int docId) { return frequency.containsKey(docId); }
    }

    public static class SearchResult implements Comparable<SearchResult> {
        public final int docId;
        public final double score;
        public final List<String> missingKeywords;   // query terms not in this document

        public SearchResult(int docId, double score, List<String> missingKeywords) {
            this.docId = docId;
            this.score = score;
            this.missingKeywords = missingKeywords;
        }

        @Override
        public int compareTo(SearchResult o) {
            int byScore = Double.compare(o.score, this.score);
            if (byScore != 0) return byScore;
            return Integer.compare(this.docId, o.docId);
        }
    }

    private static final PostingList EMPTY_POSTING_LIST = new PostingList() {
        @Override public Set<Integer> getPageIds() { return Collections.emptySet(); }
        @Override public int getTermFrequency(int docId) { return 0; }
        @Override public List<Integer> getPositions(int docId) { return Collections.emptyList(); }
        @Override public int getDocumentFrequency() { return 0; }
        // add other abstract methods if any
    };
}

package com.comp4321.search;

import com.comp4321.spider.indexer.PostingList;
import com.comp4321.spider.indexer.PageMeta;
import com.comp4321.spider.indexer.StopStem;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.htree.HTree;
import java.io.*;
import java.util.*;

public class SimpleIndexer {
    public static void main(String[] args) throws Exception {
        String dbPath = "testIndexDB";
        RecordManager recman = RecordManagerFactory.createRecordManager(dbPath);
        
        HTree wordToWordId = HTree.createInstance(recman);
        HTree bodyInvertedIndex = HTree.createInstance(recman);
        HTree pageMetadata = HTree.createInstance(recman);
        
        // Save named objects (JDBM 1.0 style – store recids)
        recman.setNamedObject("wordToWordId", wordToWordId.getRecid());
        recman.setNamedObject("bodyInvertedIndex", bodyInvertedIndex.getRecid());
        recman.setNamedObject("pageMetadata", pageMetadata.getRecid());
        
        StopStem stopStem = new StopStem("stopwords.txt");
        
        // Documents to index
        Map<Integer, String> docs = new LinkedHashMap<>();
        docs.put(1, "new york news");
        docs.put(2, "news today");
        docs.put(3, "new car");
        docs.put(4, "hong kong");
        
        // Temporary structures
        Map<String, Map<Integer, List<Integer>>> inverted = new HashMap<>(); // stem -> docId -> positions
        Map<Integer, Map<String, Integer>> docTermFreqs = new HashMap<>();
        Map<Integer, List<String>> docTokens = new HashMap<>();
        
        for (Map.Entry<Integer, String> entry : docs.entrySet()) {
            int docId = entry.getKey();
            String text = entry.getValue();
            String[] tokens = text.toLowerCase().split("\\W+");
            Map<String, Integer> freq = new HashMap<>();
            List<String> stemmedList = new ArrayList<>();
            int pos = 0;
            for (String token : tokens) {
                if (token.isEmpty()) continue;
                if (!stopStem.isStopWord(token)) {
                    String stem = stopStem.stem(token);
                    if (!stem.isEmpty()) {
                        stemmedList.add(stem);
                        freq.put(stem, freq.getOrDefault(stem, 0) + 1);
                        inverted.computeIfAbsent(stem, k -> new HashMap<>())
                                .computeIfAbsent(docId, k -> new ArrayList<>()).add(pos);
                    }
                }
                pos++;
            }
            docTermFreqs.put(docId, freq);
            docTokens.put(docId, stemmedList);
        }
        
        // Assign wordIds and store PostingLists
        int nextWordId = 1;
        Map<String, Integer> stemToWordId = new HashMap<>();
        for (String stem : inverted.keySet()) {
            stemToWordId.put(stem, nextWordId);
            wordToWordId.put(stem, nextWordId);
            Map<Integer, List<Integer>> docPos = inverted.get(stem);
            PostingList pl = new PostingList();
            for (Map.Entry<Integer, List<Integer>> docEntry : docPos.entrySet()) {
                for (int pos : docEntry.getValue()) {
                    pl.addOccurrence(docEntry.getKey(), pos);
                }
            }
            bodyInvertedIndex.put(nextWordId, pl);
            nextWordId++;
        }
        
        // Compute max TF and norm for each doc, store PageMeta
        int totalDocs = docs.size();
        Map<String, Double> idf = new HashMap<>();
        for (String stem : inverted.keySet()) {
            int df = inverted.get(stem).size();
            idf.put(stem, Math.log((double) totalDocs / df));
        }
        
        for (int docId : docs.keySet()) {
            Map<String, Integer> freq = docTermFreqs.get(docId);
            double maxTf = freq.values().stream().max(Integer::compare).orElse(1);
            double sumSq = 0.0;
            for (Map.Entry<String, Integer> f : freq.entrySet()) {
                double tf = f.getValue();
                double weight = (tf / maxTf) * idf.getOrDefault(f.getKey(), 0.0);
                sumSq += weight * weight;
            }
            double norm = Math.sqrt(sumSq);
            PageMeta meta = new PageMeta();
            meta.pageId = docId;
            meta.url = "test";
            // Store maxTf and norm as custom fields – we need to add these fields to PageMeta or use a wrapper.
            // For now, we'll store them in a separate map because PageMeta lacks these fields.
            // Alternatively, we can store them as attributes in PageMeta if you extend it.
            // But to keep it simple, we'll store a dummy record.
            // Actually, let's extend PageMeta temporarily: we'll just add fields to PageMeta source.
            // I'll instead create a wrapper object that holds PageMeta + maxTf + norm, but we must store as one object.
            // Simpler: add two fields to PageMeta.java (maxTermFrequency, docNorm) and set them here.
            // I'll assume you already added those fields and getters.
            meta.maxTermFrequency = maxTf;
            meta.docNorm = norm;
            pageMetadata.put(docId, meta);
        }
        
        recman.commit();
        recman.close();
        System.out.println("Index created at " + dbPath);
    }
}

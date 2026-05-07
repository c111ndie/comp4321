package com.comp4321.search;
import jdbm.helper.FastIterator;
import com.comp4321.spider.indexer.StopStem;
import com.comp4321.spider.indexer.PageMeta;
import com.comp4321.spider.indexer.PostingList;
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.htree.HTree;
import java.util.stream.Collectors;

import java.io.*;
import java.util.*;

public class QueryParser {
        private final StopStem stopStem;
        private final HTree wordToWordId;

        public QueryParser(StopStem stopStem) {
            this(stopStem, null);
        }

        public QueryParser(StopStem stopStem, HTree wordToWordId) {
            this.stopStem = stopStem;
            this.wordToWordId = wordToWordId;
        }

        public Query parse(String rawQuery) {
            List<String> singleTerms = new ArrayList<>();
            List<List<String>> phrases = new ArrayList<>();

            List<String> tokens = tokenizePreserveQuotes(rawQuery);
            for (String token : tokens) {
                if (token.startsWith("\"") && token.endsWith("\"") && token.length() > 1) {
                    String phraseContent = token.substring(1, token.length() - 1);
                    String[] words = phraseContent.split("\\s+");
                    List<String> stemmedPhrase = new ArrayList<>();
                    for (String w : words) {
                        String lower = w.toLowerCase();
                        if (!stopStem.isStopWord(lower)) {
                            //if (!lower.isEmpty()) stemmedPhrase.add(lower);  // use original word, not stem
                            String stem = normalizeQueryTerm(lower);
                            if (!stem.isEmpty()) stemmedPhrase.add(stem);
                        }
                    }
                    if (!stemmedPhrase.isEmpty()) phrases.add(stemmedPhrase);
                } else {
                    String lower = token.toLowerCase();
                    if (!stopStem.isStopWord(lower)) {
                        //if (!lower.isEmpty()) singleTerms.add(lower);  // use original word, not stem
                        String stem = normalizeQueryTerm(lower);
                        if (!stem.isEmpty()) singleTerms.add(stem);
                    }
                }
            }
            return new Query(singleTerms, phrases);
        }

        private String normalizeQueryTerm(String lower) {
            if (wordToWordId != null) {
                try {
                    if (wordToWordId.get(lower) != null) {
                        return lower;
                    }
                } catch (IOException ignored) {
                    // Fall back to normal stemming if the index lookup fails.
                }
            }
            return stopStem.stem(lower);
        }

        private List<String> tokenizePreserveQuotes(String text) {
            List<String> result = new ArrayList<>();
            boolean inQuotes = false;
            StringBuilder current = new StringBuilder();
            for (char c : text.toCharArray()) {
                if (c == '"') {
                    if (inQuotes) {
                        current.append(c);
                        result.add(current.toString());
                        current.setLength(0);
                        inQuotes = false;
                    } else {
                        if (current.length() > 0) {
                            result.add(current.toString());
                            current.setLength(0);
                        }
                        current.append(c);
                        inQuotes = true;
                    }
                } else if (c == ' ' && !inQuotes) {
                    if (current.length() > 0) {
                        result.add(current.toString());
                        current.setLength(0);
                    }
                } else {
                    current.append(c);
                }
            }
            if (current.length() > 0) {
                result.add(current.toString());
            }
            return result;
        }
    }

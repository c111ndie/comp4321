package com.comp4321.spider.indexer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;

public class StopStem {
    private final Porter porter;
    private final HashSet<String> stopWords;

    public boolean isStopWord(String word) {
        return stopWords.contains(word.toLowerCase());
    }

    /**
     * Loads stopwords from a file path. If the file doesn't exist at the given path,
     * falls back to loading from the classpath (e.g., bundled in the JAR).
     */
    public StopStem(String str) {
        porter = new Porter();
        stopWords = new HashSet<>();
        try {
            InputStream is = null;
            File f = new File(str);
            if (f.exists()) {
                is = new FileInputStream(f);
            } else {
                // Try classpath (e.g., bundled stopwords.txt in the JAR)
                is = StopStem.class.getClassLoader().getResourceAsStream(str);
                if (is == null) {
                    // Try just the filename portion
                    String filename = f.getName();
                    is = StopStem.class.getClassLoader().getResourceAsStream(filename);
                }
            }
            if (is == null) {
                System.err.println("Warning: stopwords file not found: " + str);
                return;
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String word;
                while ((word = br.readLine()) != null) {
                    word = word.trim().toLowerCase();
                    if (!word.isEmpty()) stopWords.add(word);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading stopwords from: " + str);
            e.printStackTrace();
        }
    }

    public String stem(String str) {
        if (str == null || str.isEmpty()) return "";
        return porter.stripAffixes(str.toLowerCase());
    }
}

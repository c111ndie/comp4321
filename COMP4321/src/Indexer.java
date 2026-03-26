import java.io.*;
import java.util.*;
import jdbm.*;
import jdbm.htree.HTree.*;

public class Indexer {

    private StopStem stopStem;
    private InvertedIndex index;

    public Indexer(String stopwordFile, String dbName) throws IOException {
        stopStem = new StopStem(stopwordFile);
        index = new InvertedIndex(dbName);
    }

    public void indexDocument(String docId, String title, String body) throws IOException {
        String[] titleTerms = title.split("\\W+");
        String[] bodyTerms = body.split("\\W+");

        processTerms(docId, titleTerms, true);
        processTerms(docId, bodyTerms, false);
    }

    private void processTerms(String docId, String[] terms, boolean isTitle) throws IOException {
        for (String raw : terms) {
            if (raw == null || raw.isEmpty()) continue;
            String word = raw.toLowerCase();

            if (!stopStem.isStopWord(word)) {
                String stem = stopStem.stem(word);
                if (stem.isEmpty()) continue;
                index.addWord(stem, docId, isTitle);
            }
        }
    }

    public void close() throws IOException {
        index.close();
    }

    public static void main(String[] args) {
        try {
            String stopFile = "stopwords.txt";
            String dbName = "indexDB";
            Indexer idx = new Indexer(stopFile, dbName);

            /*Replace with crawler*********
            idx.indexDocument("doc1", "Introduction to Information Retrieval",
                    "This course covers indexing, vector space models, and ranking algorithms.");
            idx.indexDocument("doc2", "Porter Stemming Example",
                    "Stemming algorithms are used to normalize words into their root form.");

            idx.close();

            System.out.println("✅ Indexing complete!");*/


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

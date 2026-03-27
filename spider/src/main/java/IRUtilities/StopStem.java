package IRUtilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lab 3 style stopword removal + Porter stemming.
 * The lab exercise requires reading stopwords from a file (stopwords.txt) using BufferedReader.
 */
public final class StopStem {
    private final Set<String> stopwords;
    private final Porter porter;

    public StopStem(Set<String> stopwords) {
        this.stopwords = stopwords;
        this.porter = new Porter();
    }

    public static StopStem fromStopwordsFile(Path stopwordsPath) throws IOException {
        return new StopStem(readStopwords(stopwordsPath));
    }

    public static StopStem fromClasspathResource(String resourcePath) throws IOException {
        try (InputStream in = StopStem.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            Set<String> s = new HashSet<>();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String w = normalize(line);
                    if (!w.isEmpty()) {
                        s.add(w);
                    }
                }
            }
            return new StopStem(s);
        }
    }

    public List<String> processTokens(List<String> tokens) {
        List<String> out = new ArrayList<>();
        for (String t : tokens) {
            String w = normalize(t);
            if (w.isEmpty() || stopwords.contains(w)) {
                continue;
            }
            out.add(porter.stem(w));
        }
        return out;
    }

    private static Set<String> readStopwords(Path path) throws IOException {
        Set<String> out = new HashSet<>();
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                String w = normalize(line);
                if (!w.isEmpty()) {
                    out.add(w);
                }
            }
        }
        return out;
    }

    private static String normalize(String s) {
        if (s == null) {
            return "";
        }
        String w = s.strip().toLowerCase(Locale.ROOT);
        w = w.replaceAll("^[^a-z0-9]+|[^a-z0-9]+$", "");
        return w;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: StopStem <stopwords.txt> <token> [token...]");
            System.exit(2);
        }
        StopStem ss = StopStem.fromStopwordsFile(Path.of(args[0]));
        List<String> in = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            in.add(args[i]);
        }
        System.out.println(ss.processTokens(in));
    }
}


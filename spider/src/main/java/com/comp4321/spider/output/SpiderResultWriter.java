package com.comp4321.spider.output;

import com.comp4321.spider.store.PageRecord;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SpiderResultWriter {
    private static final String SEPARATOR = "||||||||||||||||||||||||||||||-";

    public void write(Path outPath, Map<Integer, PageRecord> pagesByIdAscending) throws IOException {
        List<Integer> ids = new ArrayList<>(pagesByIdAscending.keySet());
        ids.sort(Comparator.naturalOrder());

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            PageRecord page = pagesByIdAscending.get(ids.get(i));
            if (page == null) {
                continue;
            }
            sb.append(safe(page.title)).append('\n');
            sb.append(safe(page.url)).append('\n');

            String lm = (page.lastModifiedRfc1123 == null || page.lastModifiedRfc1123.isBlank()) ? "N/A" : page.lastModifiedRfc1123;
            sb.append(lm).append(",").append(page.sizeChars).append('\n');

            sb.append('\n');

            Set<String> outLinks = page.outLinks;
            if (outLinks != null) {
                int j = 0;
                for (String link : outLinks) {
                    sb.append(link).append('\n');
                    if (++j >= 10) {
                        break;
                    }
                }
            }

            if (i != ids.size() - 1) {
                sb.append(SEPARATOR).append('\n');
            }
        }

        Files.createDirectories(outPath.getParent());
        Files.writeString(outPath, sb.toString(), StandardCharsets.UTF_8);
    }

    private static String safe(String s) {
        return (s == null) ? "" : s.strip();
    }
}


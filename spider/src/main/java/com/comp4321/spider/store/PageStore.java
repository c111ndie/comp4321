package com.comp4321.spider.store;

import com.comp4321.spider.util.HttpDates;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;

public final class PageStore {
    private final Path outDir;
    private final Path pagesDir;
    private final Path statePath;
    private final ObjectMapper mapper;
    private CrawlState state;

    public PageStore(Path outDir) throws IOException {
        this.outDir = outDir;
        this.pagesDir = outDir.resolve("pages");
        this.statePath = outDir.resolve("state.json");
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        Files.createDirectories(this.pagesDir);
        this.state = loadState().orElseGet(CrawlState::new);
        normalizeMaps();
        normalizeRecords();
        recomputeParentLinks();
    }

    private void normalizeMaps() {
        if (state.urlToPageId == null) {
            state.urlToPageId = new LinkedHashMap<>();
        }
        if (state.pages == null) {
            state.pages = new LinkedHashMap<>();
        }
    }

    private void normalizeRecords() {
        for (PageRecord r : state.pages.values()) {
            if (r == null) {
                continue;
            }
            if (r.outLinks == null) {
                r.outLinks = new LinkedHashSet<>();
            }
            if (r.parentPageIds == null) {
                r.parentPageIds = new LinkedHashSet<>();
            }
            if (r.url == null) {
                r.url = "";
            }
            if (r.title == null) {
                r.title = "";
            }
            if (r.lastModifiedRfc1123 == null) {
                r.lastModifiedRfc1123 = "";
            }
        }
    }

    public Optional<PageRecord> getByUrl(URI url) {
        Integer id = state.urlToPageId.get(url.toString());
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(state.pages.get(id));
    }

    public Optional<PageRecord> getByPageId(int pageId) {
        return Optional.ofNullable(state.pages.get(pageId));
    }

    public Optional<Integer> pageIdForUrl(String url) {
        if (url == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(state.urlToPageId.get(url));
    }

    public PageRecord getOrCreate(URI url) {
        String key = url.toString();
        Integer existing = state.urlToPageId.get(key);
        if (existing != null) {
            PageRecord record = state.pages.get(existing);
            if (record != null) {
                return record;
            }
        }
        int id = state.nextPageId++;
        PageRecord created = new PageRecord(id, key);
        created.title = "";
        created.lastModifiedRfc1123 = "";
        created.sizeChars = 0;
        created.isHtml = false;
        created.outLinks = new LinkedHashSet<>();
        created.parentPageIds = new LinkedHashSet<>();
        state.urlToPageId.put(key, id);
        state.pages.put(id, created);
        return created;
    }

    public void saveHtml(PageRecord page, String html) throws IOException {
        if (page == null) {
            return;
        }
        if (html == null) {
            html = "";
        }
        Path p = pagesDir.resolve(page.pageId + ".html");
        Files.writeString(p, html, StandardCharsets.UTF_8);
    }

    public Optional<String> readHtml(PageRecord page) {
        if (page == null) {
            return Optional.empty();
        }
        Path p = pagesDir.resolve(page.pageId + ".html");
        if (!Files.exists(p)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readString(p, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public Path getOutDir() {
        return outDir;
    }

    public Map<Integer, PageRecord> pagesByIdAscending() {
        Map<Integer, PageRecord> out = new LinkedHashMap<>();
        state.pages.entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .forEach(e -> out.put(e.getKey(), e.getValue()));
        return out;
    }

    public int pageCount() {
        return state.pages.size();
    }

    public Optional<Instant> lastModifiedInstant(PageRecord record) {
        if (record == null) {
            return Optional.empty();
        }
        if (!record.lastModifiedFromHeader) {
            return Optional.empty();
        }
        return HttpDates.parseRfc1123(record.lastModifiedRfc1123);
    }

    public void recomputeParentLinks() {
        normalizeRecords();
        for (PageRecord r : state.pages.values()) {
            if (r == null) {
                continue;
            }
            r.parentPageIds.clear();
        }

        for (PageRecord parent : state.pages.values()) {
            if (parent == null || parent.outLinks == null) {
                continue;
            }
            int parentId = parent.pageId;
            for (String childUrl : parent.outLinks) {
                if (childUrl == null || childUrl.isBlank()) {
                    continue;
                }
                Integer childId = state.urlToPageId.get(childUrl);
                if (childId == null) {
                    continue;
                }
                PageRecord child = state.pages.get(childId);
                if (child == null || child.parentPageIds == null) {
                    continue;
                }
                child.parentPageIds.add(parentId);
            }
        }
    }

    public void checkpoint() throws IOException {
        Path tmp = outDir.resolve("state.json.tmp");
        try (OutputStream os = Files.newOutputStream(tmp)) {
            mapper.writeValue(os, state);
        }
        Files.move(tmp, statePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private Optional<CrawlState> loadState() {
        if (!Files.exists(statePath)) {
            return Optional.empty();
        }
        try (InputStream in = Files.newInputStream(statePath)) {
            return Optional.of(mapper.readValue(in, CrawlState.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}

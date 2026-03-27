package com.comp4321.spider;

import static org.junit.jupiter.api.Assertions.*;

import com.comp4321.spider.store.PageRecord;
import com.comp4321.spider.store.PageStore;
import java.net.URI;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class PageStoreParentLinksTest {
    @TempDir
    Path tempDir;

    @Test
    void recomputeParentLinks_buildsIncomingEdges_and_persists() throws Exception {
        PageStore store = new PageStore(tempDir);

        PageRecord a = store.getOrCreate(URI.create("https://example.com/a.html"));
        PageRecord b = store.getOrCreate(URI.create("https://example.com/b.html"));
        PageRecord c = store.getOrCreate(URI.create("https://example.com/c.html"));

        a.outLinks.add(b.url);
        a.outLinks.add(c.url);
        b.outLinks.add(c.url);

        store.recomputeParentLinks();

        assertEquals(Set.of(a.pageId), b.parentPageIds);
        assertEquals(Set.of(a.pageId, b.pageId), c.parentPageIds);
        assertTrue(a.parentPageIds.isEmpty());

        store.checkpoint();

        PageStore store2 = new PageStore(tempDir);
        PageRecord b2 = store2.getByUrl(URI.create(b.url)).orElseThrow();
        PageRecord c2 = store2.getByUrl(URI.create(c.url)).orElseThrow();

        assertEquals(Set.of(a.pageId), b2.parentPageIds);
        assertEquals(Set.of(a.pageId, b.pageId), c2.parentPageIds);
    }
}


package com.comp4321.spider;

import static org.junit.jupiter.api.Assertions.*;

import com.comp4321.spider.util.UrlCanonicalizer;
import java.net.URI;
import org.junit.jupiter.api.Test;

public class UrlCanonicalizerTest {
    @Test
    void canonicalize_stripsFragment_and_normalizesHostAndDefaultPort() {
        URI in = URI.create("HTTP://Example.COM:80/a/b/../c.html#section");
        URI out = UrlCanonicalizer.canonicalize(in).orElseThrow();
        assertEquals("http", out.getScheme());
        assertEquals("example.com", out.getHost());
        assertEquals(-1, out.getPort());
        assertEquals("/a/c.html", out.getPath());
        assertNull(out.getFragment());
    }

    @Test
    void resolveAndCanonicalize_resolvesRelative() {
        URI base = URI.create("https://example.com/dir/page.html");
        URI out = UrlCanonicalizer.resolveAndCanonicalize(base, "../x.html").orElseThrow();
        assertEquals("https://example.com/x.html", out.toString());
    }

    @Test
    void canonicalize_rejectsNonHttpSchemes() {
        assertTrue(UrlCanonicalizer.canonicalize(URI.create("mailto:test@example.com")).isEmpty());
        assertTrue(UrlCanonicalizer.canonicalize(URI.create("javascript:alert(1)")).isEmpty());
    }
}


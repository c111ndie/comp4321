package com.comp4321.spider;

import static org.junit.jupiter.api.Assertions.*;

import com.comp4321.spider.parse.HtmlParserAdapter;
import java.net.URI;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class HtmlParserAdapterTest {
    @Test
    void extractsTitleAndLinks() {
        String html = "<html><head><title> Hello </title></head><body>"
                + "<a href=\"/a.html\">A</a>"
                + "<a href=\"b.html#frag\">B</a>"
                + "<a href=\"mailto:test@example.com\">M</a>"
                + "</body></html>";

        HtmlParserAdapter p = new HtmlParserAdapter();
        assertEquals("Hello", p.extractTitle(html));

        Set<URI> links = p.extractLinks(html, URI.create("https://example.com/dir/index.html"));
        assertTrue(links.contains(URI.create("https://example.com/a.html")));
        assertTrue(links.contains(URI.create("https://example.com/dir/b.html")));
        assertEquals(2, links.size());
    }
}


package com.comp4321.spider.labs;

import com.comp4321.spider.util.UrlCanonicalizer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TitleTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.TextExtractingVisitor;

/**
 * Lab-2 style "Crawler.java" facade used by the Phase-1 spider.
 *
 * The lab handout focuses on two main functions:
 * - extractWords(): extract tokenized words from a page
 * - extractLinks(): extract hyperlinks from a page (resolved against base URL)
 *
 * This implementation parses an already-fetched HTML string so the spider can still control
 * HTTP fetching, headers, persistence, and BFS scheduling.
 */
public final class Crawler {
    public Vector<String> extractWords(String html) {
        Vector<String> out = new Vector<>();
        if (html == null || html.isBlank()) {
            return out;
        }
        String text = extractText(html);
        StringTokenizer st = new StringTokenizer(text);
        while (st.hasMoreTokens()) {
            out.add(st.nextToken());
        }
        return out;
    }

    public Vector<URI> extractLinks(String html, URI base) {
        Vector<URI> out = new Vector<>();
        if (html == null || html.isBlank()) {
            return out;
        }

        Set<URI> deduped = new LinkedHashSet<>();
        try {
            Parser parser = Parser.createParser(html, StandardCharsets.UTF_8.name());
            NodeList nodes = parser.extractAllNodesThatMatch(new NodeClassFilter(LinkTag.class));
            for (Node node : nodes.toNodeArray()) {
                if (!(node instanceof LinkTag)) {
                    continue;
                }
                String href = ((LinkTag) node).getLink();
                Optional<URI> u = UrlCanonicalizer.resolveAndCanonicalize(base, href);
                u.ifPresent(deduped::add);
            }
        } catch (ParserException ignored) {
            return out;
        }

        out.addAll(deduped);
        return out;
    }

    public String extractTitle(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        try {
            Parser parser = Parser.createParser(html, StandardCharsets.UTF_8.name());
            NodeList nodes = parser.extractAllNodesThatMatch(new NodeClassFilter(TitleTag.class));
            for (Node node : nodes.toNodeArray()) {
                if (node instanceof TitleTag) {
                    String title = ((TitleTag) node).getTitle();
                    return (title == null) ? "" : title.trim();
                }
            }
            return "";
        } catch (ParserException e) {
            return "";
        }
    }

    private static String extractText(String html) {
        try {
            Parser parser = Parser.createParser(html, StandardCharsets.UTF_8.name());
            TextExtractingVisitor visitor = new TextExtractingVisitor();
            parser.visitAllNodesWith(visitor);
            return visitor.getExtractedText();
        } catch (ParserException e) {
            return "";
        }
    }
}


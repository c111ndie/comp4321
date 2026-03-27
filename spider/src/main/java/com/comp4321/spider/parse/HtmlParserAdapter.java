package com.comp4321.spider.parse;

import com.comp4321.spider.util.UrlCanonicalizer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TitleTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

public final class HtmlParserAdapter {
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

    public Set<URI> extractLinks(String html, URI base) {
        Set<URI> out = new LinkedHashSet<>();
        if (html == null || html.isBlank()) {
            return out;
        }
        try {
            Parser parser = Parser.createParser(html, StandardCharsets.UTF_8.name());
            NodeList nodes = parser.extractAllNodesThatMatch(new NodeClassFilter(LinkTag.class));
            for (Node node : nodes.toNodeArray()) {
                if (!(node instanceof LinkTag)) {
                    continue;
                }
                LinkTag link = (LinkTag) node;
                String href = link.getLink();
                Optional<URI> u = UrlCanonicalizer.resolveAndCanonicalize(base, href);
                u.ifPresent(out::add);
            }
        } catch (ParserException ignored) {
            return out;
        }
        return out;
    }
}


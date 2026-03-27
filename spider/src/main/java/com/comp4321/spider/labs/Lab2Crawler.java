package com.comp4321.spider.labs;

import java.net.URL;
import java.util.StringTokenizer;
import java.util.Vector;
import org.htmlparser.beans.LinkBean;
import org.htmlparser.beans.StringBean;

/**
 * Lab 2 style helper (HTMLParser):
 * - extractWords(): fetch page text then tokenize
 * - extractLinks(): extract (absolute) hyperlinks from the page
 *
 * This is included to align with COMP4321 lab materials; the main spider uses HttpClient + HTMLParser parsing.
 */
public final class Lab2Crawler {
    public Vector<String> extractWords(String url) {
        StringBean sb = new StringBean();
        sb.setLinks(false);
        sb.setReplaceNonBreakingSpaces(true);
        sb.setCollapse(true);
        sb.setURL(url);

        String text = sb.getStrings();
        Vector<String> out = new Vector<>();
        StringTokenizer st = new StringTokenizer(text);
        while (st.hasMoreTokens()) {
            out.add(st.nextToken());
        }
        return out;
    }

    public Vector<String> extractLinks(String url) {
        LinkBean lb = new LinkBean();
        lb.setURL(url);

        Vector<String> out = new Vector<>();
        URL[] links = lb.getLinks();
        if (links == null) {
            return out;
        }
        for (URL u : links) {
            if (u != null) {
                out.add(u.toString());
            }
        }
        return out;
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: Lab2Crawler <url>");
            System.exit(2);
        }
        String url = args[0];
        Lab2Crawler c = new Lab2Crawler();
        Vector<String> words = c.extractWords(url);
        System.out.println("Words in " + url + " (size=" + words.size() + "):");
        for (int i = 0; i < Math.min(words.size(), 80); i++) {
            System.out.println(words.get(i));
        }
        Vector<String> links = c.extractLinks(url);
        System.out.println("Links in " + url + ":");
        for (String l : links) {
            System.out.println(l);
        }
    }
}


package com.comp4321.spider.store;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class LinkGraph {
    private final PageStore store;

    public LinkGraph(PageStore store) {
        this.store = store;
    }

    public Set<String> childrenOf(int pageId) {
        PageRecord r = store.pagesByIdAscending().get(pageId);
        if (r == null || r.outLinks == null) {
            return Collections.emptySet();
        }
        return new LinkedHashSet<>(r.outLinks);
    }

    public Set<Integer> childrenPageIdsOf(int pageId) {
        PageRecord r = store.pagesByIdAscending().get(pageId);
        if (r == null || r.outLinks == null) {
            return Collections.emptySet();
        }
        Set<Integer> out = new LinkedHashSet<>();
        for (String childUrl : r.outLinks) {
            store.pageIdForUrl(childUrl).ifPresent(out::add);
        }
        return out;
    }

    public Set<Integer> parentPageIdsOf(int pageId) {
        PageRecord r = store.pagesByIdAscending().get(pageId);
        if (r == null || r.parentPageIds == null) {
            return Collections.emptySet();
        }
        return new LinkedHashSet<>(r.parentPageIds);
    }
}

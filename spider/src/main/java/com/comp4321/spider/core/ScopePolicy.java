package com.comp4321.spider.core;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;

@FunctionalInterface
public interface ScopePolicy {
    boolean allows(URI url);

    static ScopePolicy sameHostOnly(URI seed) {
        String seedHost = seed.getHost();
        if (seedHost == null) {
            throw new IllegalArgumentException("Seed URL must include a host: " + seed);
        }
        String normalized = seedHost.toLowerCase(Locale.ROOT);
        return url -> {
            String host = url.getHost();
            return host != null && host.toLowerCase(Locale.ROOT).equals(normalized);
        };
    }

    static ScopePolicy and(ScopePolicy a, ScopePolicy b) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);
        return url -> a.allows(url) && b.allows(url);
    }
}


package com.comp4321.spider.core;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

public final class SpiderConfig {
    public final URI seed;
    public final int maxPages;
    public final Path outDir;
    public final Duration politenessDelay;
    public final String userAgent;
    public final ScopePolicy scopePolicy;

    public SpiderConfig(
            URI seed,
            int maxPages,
            Path outDir,
            Duration politenessDelay,
            String userAgent,
            ScopePolicy scopePolicy
    ) {
        this.seed = Objects.requireNonNull(seed);
        if (maxPages <= 0) {
            throw new IllegalArgumentException("maxPages must be > 0");
        }
        this.maxPages = maxPages;
        this.outDir = Objects.requireNonNull(outDir);
        this.politenessDelay = Objects.requireNonNull(politenessDelay);
        this.userAgent = Objects.requireNonNull(userAgent);
        this.scopePolicy = Objects.requireNonNull(scopePolicy);
    }
}


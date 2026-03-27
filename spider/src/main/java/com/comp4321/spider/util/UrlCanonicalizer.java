package com.comp4321.spider.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;

public final class UrlCanonicalizer {
    private UrlCanonicalizer() {}

    public static Optional<URI> canonicalize(URI url) {
        if (url == null) {
            return Optional.empty();
        }
        String scheme = url.getScheme();
        if (scheme == null) {
            return Optional.empty();
        }
        scheme = scheme.toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            return Optional.empty();
        }
        String host = url.getHost();
        if (host == null) {
            return Optional.empty();
        }

        int port = url.getPort();
        if ((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443)) {
            port = -1;
        }

        String path = url.getRawPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }

        try {
            URI rebuilt = new URI(
                    scheme,
                    url.getRawUserInfo(),
                    host.toLowerCase(Locale.ROOT),
                    port,
                    path,
                    url.getRawQuery(),
                    null
            ).normalize();
            return Optional.of(rebuilt);
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }

    public static Optional<URI> resolveAndCanonicalize(URI base, String href) {
        if (href == null || href.isBlank()) {
            return Optional.empty();
        }
        String rawHref = href.trim();
        String lower = rawHref.toLowerCase(Locale.ROOT);
        if (lower.startsWith("mailto:") || lower.startsWith("javascript:")) {
            return Optional.empty();
        }

        // Some HTML parsers expose email links as plain "user@example.com" (without "mailto:").
        // Treat these as non-crawlable.
        if (rawHref.indexOf('@') >= 0 && !lower.contains("://") && !lower.startsWith("/") && !lower.startsWith("#")
                && !lower.startsWith("?") && !lower.startsWith("//")) {
            return Optional.empty();
        }
        try {
            URI raw = URI.create(rawHref);
            URI resolved = (base == null) ? raw : base.resolve(raw);
            return canonicalize(resolved);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

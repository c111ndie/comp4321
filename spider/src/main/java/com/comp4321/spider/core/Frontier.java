package com.comp4321.spider.core;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

public final class Frontier {
    private final Queue<URI> queue = new ArrayDeque<>();
    private final Set<URI> queued = new HashSet<>();
    private final Set<URI> seen = new HashSet<>();

    public void enqueue(URI url) {
        Objects.requireNonNull(url);
        if (seen.contains(url) || queued.contains(url)) {
            return;
        }
        queue.add(url);
        queued.add(url);
    }

    public Optional<URI> dequeue() {
        URI next = queue.poll();
        if (next == null) {
            return Optional.empty();
        }
        queued.remove(next);
        seen.add(next);
        return Optional.of(next);
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int seenCount() {
        return seen.size();
    }
}


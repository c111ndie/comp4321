package com.comp4321.spider;

import static org.junit.jupiter.api.Assertions.*;

import com.comp4321.spider.core.Frontier;
import java.net.URI;
import org.junit.jupiter.api.Test;

public class FrontierTest {
    @Test
    void bfs_order_and_dedupe() {
        Frontier f = new Frontier();
        URI a = URI.create("https://example.com/a");
        URI b = URI.create("https://example.com/b");
        URI c = URI.create("https://example.com/c");

        f.enqueue(a);
        f.enqueue(b);
        f.enqueue(a);
        f.enqueue(c);

        assertEquals(a, f.dequeue().orElseThrow());
        assertEquals(b, f.dequeue().orElseThrow());
        assertEquals(c, f.dequeue().orElseThrow());
        assertTrue(f.isEmpty());
    }
}


package com.typeahead;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Search Typeahead application.
 *
 * Features:
 *  - Trie-based prefix matching for search suggestions (O(P+N))
 *  - H2 database persistence — hydrates Trie on startup; falls back to CSV on first run
 *  - Consistent hash ring routing across simulated cache nodes
 *  - Batch writing with aggregation to minimise Trie lock contention
 *  - Trending service with historical and recency-weighted modes + decay
 *  - Metrics endpoint exposing latency, cache hit rate, and write reduction
 */
@SpringBootApplication
@EnableScheduling
public class SearchTypeaheadApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchTypeaheadApplication.class, args);
    }
}

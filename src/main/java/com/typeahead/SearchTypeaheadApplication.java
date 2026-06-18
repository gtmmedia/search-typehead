package com.typeahead;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Search Typeahead application.
 *
 * Phase 1 features:
 *  - Trie-based prefix matching for search suggestions
 *  - CSV dataset loading on startup
 *  - Real-time query submission with popularity tracking
 *  - Basic metrics (search count, suggestion requests, avg latency)
 */
@SpringBootApplication
@EnableScheduling
public class SearchTypeaheadApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchTypeaheadApplication.class, args);
    }
}

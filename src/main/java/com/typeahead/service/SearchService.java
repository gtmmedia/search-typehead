package com.typeahead.service;

import com.typeahead.cache.CacheCluster;
import com.typeahead.cache.CacheEntry;
import com.typeahead.model.QueryData;
import com.typeahead.trie.Trie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Core business logic for the Search Typeahead system.
 */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final Trie trie;
    private final MetricsService metricsService;
    private final CacheCluster cacheCluster;
    
    private static final long CACHE_TTL_SECONDS = 60; // 1 minute TTL

    public SearchService(Trie trie, MetricsService metricsService, CacheCluster cacheCluster) {
        this.trie = trie;
        this.metricsService = metricsService;
        this.cacheCluster = cacheCluster;
    }

    public List<QueryData> getSuggestions(String prefix) {
        long startNanos = System.nanoTime();

        try {
            if (prefix == null || prefix.isBlank()) {
                return List.of();
            }

            String normalizedPrefix = prefix.toLowerCase().trim();

            // Cache Lookup
            CacheEntry entry = cacheCluster.get(normalizedPrefix);
            if (entry != null) {
                metricsService.recordCacheHit();
                log.debug("Cache hit for '{}'", normalizedPrefix);
                return entry.getSuggestions();
            }

            // Cache Miss -> Trie Fallback
            metricsService.recordCacheMiss();
            log.debug("Cache miss for '{}'", normalizedPrefix);
            
            List<QueryData> suggestions = trie.getSuggestions(normalizedPrefix);

            // Cache Populate
            cacheCluster.put(normalizedPrefix, suggestions, CACHE_TTL_SECONDS);

            return suggestions;

        } finally {
            metricsService.recordSuggestion();
            metricsService.recordLatency(startNanos);
        }
    }

    public QueryData submitSearch(String query) {
        long startNanos = System.nanoTime();

        try {
            if (query == null || query.isBlank()) {
                throw new IllegalArgumentException("Search query cannot be null or empty");
            }
            
            String normalizedQuery = query.toLowerCase().trim();

            QueryData result = trie.insert(normalizedQuery, 1);
            
            // Invalidate overlapping cache entries
            cacheCluster.invalidateRelated(normalizedQuery);

            log.info("Search submitted: '{}' -> count: {}", result.query(), result.count());
            return result;

        } finally {
            metricsService.recordSearch();
            metricsService.recordLatency(startNanos);
        }
    }

    public QueryData submitSearch(String query, int count) {
        long startNanos = System.nanoTime();

        try {
            if (query == null || query.isBlank()) {
                throw new IllegalArgumentException("Search query cannot be null or empty");
            }

            String normalizedQuery = query.toLowerCase().trim();

            QueryData result = trie.insert(normalizedQuery, count);

            // Invalidate overlapping cache entries
            cacheCluster.invalidateRelated(normalizedQuery);

            log.info("Batch search submitted: '{}' -> count added: {}, total count: {}", result.query(), count, result.count());
            return result;

        } finally {
            metricsService.recordLatency(startNanos);
        }
    }
}

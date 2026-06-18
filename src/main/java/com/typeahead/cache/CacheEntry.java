package com.typeahead.cache;

import com.typeahead.model.QueryData;

import java.time.Instant;
import java.util.List;

/**
 * Represents a single entry in the distributed cache.
 *
 * Each CacheEntry maps a prefix string to its list of suggestions
 * and carries TTL (Time-To-Live) metadata for expiration.
 *
 * Lifecycle:
 *   1. Created when a Trie lookup result is cached after a cache miss.
 *   2. Served directly on cache hits (bypassing Trie entirely).
 *   3. Expires after TTL seconds and is evicted on next access (lazy expiration).
 *   4. Invalidated explicitly when a search submission modifies the Trie.
 *
 * Immutability: The suggestions list is stored as an unmodifiable copy
 * to prevent external mutation of cached data.
 */
public class CacheEntry {

    /** The prefix string this entry caches suggestions for */
    private final String prefix;

    /** Cached suggestion results (immutable snapshot from Trie) */
    private final List<QueryData> suggestions;

    /** Timestamp when this entry was created (epoch millis) */
    private final long createdAtMillis;

    /** Time-To-Live in seconds — entry expires after this duration */
    private final long ttlSeconds;

    /**
     * Constructs a new CacheEntry.
     *
     * @param prefix      the search prefix being cached
     * @param suggestions the Trie lookup results to cache
     * @param ttlSeconds  how long this entry remains valid (in seconds)
     */
    public CacheEntry(String prefix, List<QueryData> suggestions, long ttlSeconds) {
        this.prefix = prefix;
        // Defensive copy: store as unmodifiable to prevent external mutation
        this.suggestions = List.copyOf(suggestions);
        this.createdAtMillis = System.currentTimeMillis();
        this.ttlSeconds = ttlSeconds;
    }

    // ============================================================
    // Expiration logic
    // ============================================================

    /**
     * Checks whether this cache entry has expired.
     *
     * An entry expires when the elapsed time since creation exceeds
     * the configured TTL.
     *
     * @return true if expired, false if still valid
     */
    public boolean isExpired() {
        long elapsedMillis = System.currentTimeMillis() - createdAtMillis;
        return elapsedMillis >= (ttlSeconds * 1000);
    }

    /**
     * Returns the remaining TTL in seconds before this entry expires.
     *
     * @return remaining seconds, or 0 if already expired
     */
    public long getTtlRemainingSeconds() {
        long elapsedMillis = System.currentTimeMillis() - createdAtMillis;
        long remainingMillis = (ttlSeconds * 1000) - elapsedMillis;
        return Math.max(0, remainingMillis / 1000);
    }

    // ============================================================
    // Accessors
    // ============================================================

    public String getPrefix() {
        return prefix;
    }

    public List<QueryData> getSuggestions() {
        return suggestions;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    /**
     * Returns the creation timestamp as an Instant for human-readable logging.
     */
    public Instant getCreatedAt() {
        return Instant.ofEpochMilli(createdAtMillis);
    }

    @Override
    public String toString() {
        return String.format("CacheEntry{prefix='%s', suggestions=%d, ttlRemaining=%ds}",
                prefix, suggestions.size(), getTtlRemainingSeconds());
    }
}

package com.typeahead.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a single node in the distributed cache cluster.
 *
 * In a real distributed system, each CacheNode would be a separate
 * process/server (e.g., a Redis instance). In this in-process simulation,
 * each CacheNode holds its own isolated key-value store using a
 * ConcurrentHashMap for thread-safe operations.
 *
 * Responsibilities:
 *   - Store and retrieve CacheEntry objects by prefix key
 *   - Handle lazy expiration (expired entries removed on access)
 *   - Support bulk invalidation for cache coherency
 *   - Report its own entry count for monitoring
 *
 * The consistent hash ring determines WHICH CacheNode is responsible
 * for a given prefix. Each prefix maps to exactly one node.
 *
 * Thread safety: ConcurrentHashMap provides lock-free reads and
 * segment-level locking for writes.
 */
public class CacheNode {

    private static final Logger log = LoggerFactory.getLogger(CacheNode.class);

    /** Unique identifier for this cache node (e.g., "CacheNode-1") */
    private final String nodeId;

    /**
     * The local cache store for this node.
     * Key:   normalized prefix string (lowercase)
     * Value: CacheEntry with suggestions and TTL metadata
     *
     * ConcurrentHashMap chosen over synchronized HashMap because:
     *   - Lock-free reads (suggestions are read-heavy)
     *   - Segment-level locking on writes (minimal contention)
     *   - No global lock bottleneck
     */
    private final ConcurrentHashMap<String, CacheEntry> store;

    /**
     * Constructs a new CacheNode with the given identifier.
     *
     * @param nodeId unique name for this node (e.g., "CacheNode-1")
     */
    public CacheNode(String nodeId) {
        this.nodeId = nodeId;
        this.store = new ConcurrentHashMap<>();
        log.info("CacheNode '{}' initialized", nodeId);
    }

    // ============================================================
    // GET — Retrieve with lazy expiration
    // ============================================================

    /**
     * Retrieves a cached entry for the given prefix.
     *
     * Implements lazy expiration: if the entry exists but has expired,
     * it is removed and null is returned (as if it never existed).
     * This avoids the need for a background reaper thread in Phase 2.
     *
     * @param prefix the search prefix to look up
     * @return the CacheEntry if present and not expired, null otherwise
     */
    public CacheEntry get(String prefix) {
        CacheEntry entry = store.get(prefix);

        if (entry == null) {
            return null;
        }

        // Lazy expiration: check TTL on every read
        if (entry.isExpired()) {
            store.remove(prefix);
            log.debug("[{}] Expired entry evicted for prefix '{}'", nodeId, prefix);
            return null;
        }

        return entry;
    }

    // ============================================================
    // PUT — Store an entry
    // ============================================================

    /**
     * Stores a cache entry for the given prefix, overwriting any
     * existing entry.
     *
     * @param prefix the search prefix key
     * @param entry  the CacheEntry containing suggestions and TTL
     */
    public void put(String prefix, CacheEntry entry) {
        store.put(prefix, entry);
        log.debug("[{}] Cached {} suggestions for prefix '{}' (TTL: {}s)",
                nodeId, entry.getSuggestions().size(), prefix, entry.getTtlSeconds());
    }

    // ============================================================
    // INVALIDATION — Cache coherency operations
    // ============================================================

    /**
     * Invalidates (removes) the cache entry for a specific prefix.
     *
     * Called when a search submission modifies the Trie, making
     * cached suggestions for overlapping prefixes potentially stale.
     *
     * @param prefix the prefix to invalidate
     * @return true if an entry was actually removed, false if not cached
     */
    public boolean invalidate(String prefix) {
        CacheEntry removed = store.remove(prefix);
        if (removed != null) {
            log.debug("[{}] Invalidated cache entry for prefix '{}'", nodeId, prefix);
            return true;
        }
        return false;
    }

    /**
     * Invalidates ALL entries on this node whose prefix starts with
     * or is a prefix of the given query.
     *
     * Example: If "iphone" is submitted via POST /search, we invalidate:
     *   - "i", "ip", "iph", "ipho", "iphon", "iphone"  (prefixes of the query)
     *   - "iphone 1", "iphone 15" etc. (queries starting with the submitted term)
     *
     * This ensures cache coherency: any cached suggestion list that
     * could contain the modified query is evicted.
     *
     * @param query the search query that was submitted/modified
     * @return number of entries invalidated
     */
    public int invalidateRelated(String query) {
        String normalizedQuery = query.toLowerCase().trim();
        int invalidatedCount = 0;

        // Iterate over all cached prefixes on this node
        for (String cachedPrefix : store.keySet()) {
            // Case 1: cached prefix is a prefix of the query ("iph" matches "iphone")
            // Case 2: query is a prefix of the cached prefix ("iphone" matches "iphone 15")
            if (normalizedQuery.startsWith(cachedPrefix) || cachedPrefix.startsWith(normalizedQuery)) {
                store.remove(cachedPrefix);
                invalidatedCount++;
                log.debug("[{}] Invalidated related prefix '{}' for query '{}'",
                        nodeId, cachedPrefix, normalizedQuery);
            }
        }

        return invalidatedCount;
    }

    /**
     * Clears all entries from this cache node.
     * Used during node removal from the cluster.
     */
    public void clear() {
        int size = store.size();
        store.clear();
        log.info("[{}] Cleared all {} entries", nodeId, size);
    }

    // ============================================================
    // Monitoring & Diagnostics
    // ============================================================

    /**
     * @return the number of entries currently stored (including potentially expired ones)
     */
    public int size() {
        return store.size();
    }

    /**
     * @return the unique identifier for this node
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Returns all prefix keys stored on this node.
     * Used by the debug endpoint and monitoring.
     *
     * @return set of cached prefix strings
     */
    public Set<String> getKeys() {
        return store.keySet();
    }

    /**
     * Returns all entries stored on this node.
     * Used during node removal for key redistribution.
     *
     * @return collection of all CacheEntry values
     */
    public Collection<Map.Entry<String, CacheEntry>> getEntries() {
        return store.entrySet();
    }

    @Override
    public String toString() {
        return String.format("CacheNode{id='%s', entries=%d}", nodeId, store.size());
    }
}

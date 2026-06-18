package com.typeahead.trie;

import com.typeahead.model.QueryData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Trie (prefix tree) data structure for efficient typeahead suggestions.
 *
 * Core operations:
 *   - insert(query, count):     O(L) where L = query length
 *   - getSuggestions(prefix):   O(P + N) where P = prefix length, N = matching subtree nodes
 *
 * Design decisions:
 *   1. All queries are stored lowercase for case-insensitive matching.
 *   2. The Trie is a single Spring-managed bean (@Component), shared across requests.
 *   3. Insert operations are synchronized to handle concurrent POST /search requests.
 *   4. Read operations (getSuggestions) are NOT synchronized — acceptable in Phase 1
 *      since stale reads during concurrent writes are tolerable for suggestions.
 *   5. Results are sorted descending by popularity and capped at 10.
 *
 * Phase 2+ considerations:
 *   - Read-write lock for better concurrency
 *   - Caching of hot prefixes
 *   - Concurrent trie or lock-striping
 */
@Component
public class Trie {

    private static final Logger log = LoggerFactory.getLogger(Trie.class);

    /** Maximum number of suggestions returned per request */
    private static final int MAX_SUGGESTIONS = 10;

    /** Root node of the Trie — always exists, never represents a character */
    private final TrieNode root;

    public Trie() {
        this.root = new TrieNode();
        log.info("Trie initialized with empty root node");
    }

    // ============================================================
    // INSERT
    // ============================================================

    /**
     * Inserts a query into the Trie with the given popularity count.
     *
     * If the query already exists:
     *   - The count is ADDED to the existing count (accumulative).
     *
     * If the query is new:
     *   - New nodes are created along the path.
     *   - The terminal node is marked as end-of-word with the given count.
     *
     * Thread safety: synchronized to prevent race conditions during
     * concurrent POST /search requests.
     *
     * @param query the search query (will be lowercased)
     * @param count the popularity count to set or add
     * @return the updated QueryData after insertion
     */
    public synchronized QueryData insert(String query, long count) {
        // Normalize to lowercase for case-insensitive matching
        String normalizedQuery = query.toLowerCase().trim();

        if (normalizedQuery.isEmpty()) {
            log.warn("Attempted to insert empty query, skipping");
            return null;
        }

        // Traverse the Trie, creating nodes as needed
        TrieNode current = root;
        for (char ch : normalizedQuery.toCharArray()) {
            if (!current.hasChild(ch)) {
                current.putChild(ch, new TrieNode());
            }
            current = current.getChild(ch);
        }

        // Mark the terminal node as end-of-word
        if (current.isEndOfWord()) {
            // Query already exists — accumulate the count
            long newCount = current.getCount() + count;
            current.setCount(newCount);
            log.debug("Updated existing query '{}': count {} -> {}", normalizedQuery, current.getCount() - count, newCount);
        } else {
            // New query — initialize
            current.setEndOfWord(true);
            current.setQuery(normalizedQuery);
            current.setCount(count);
            log.debug("Inserted new query '{}' with count {}", normalizedQuery, count);
        }

        return new QueryData(current.getQuery(), current.getCount());
    }

    // ============================================================
    // GET SUGGESTIONS
    // ============================================================

    /**
     * Returns the top suggestions matching the given prefix, sorted
     * descending by popularity count.
     *
     * Algorithm:
     *   1. Navigate to the node representing the last character of the prefix.
     *   2. If the prefix doesn't exist in the Trie, return empty list.
     *   3. DFS from that node to collect all end-of-word descendants.
     *   4. Sort by count descending.
     *   5. Return top MAX_SUGGESTIONS results.
     *
     * @param prefix the search prefix (will be lowercased)
     * @return list of matching QueryData, sorted by count descending, max 10
     */
    public List<QueryData> getSuggestions(String prefix) {
        // Normalize input
        String normalizedPrefix = prefix.toLowerCase().trim();

        if (normalizedPrefix.isEmpty()) {
            return List.of(); // Empty input → empty result
        }

        // Step 1: Navigate to the prefix node
        TrieNode prefixNode = navigateToNode(normalizedPrefix);

        if (prefixNode == null) {
            // Prefix not found in Trie — no matches
            log.debug("No Trie path found for prefix '{}'", normalizedPrefix);
            return List.of();
        }

        // Step 2: DFS to collect all end-of-word nodes in the subtree
        List<QueryData> matches = new ArrayList<>();
        collectAllWords(prefixNode, matches);

        // Step 3: Sort by count descending and limit to MAX_SUGGESTIONS
        matches.sort(Comparator.comparingLong(QueryData::count).reversed());

        List<QueryData> topResults = matches.size() > MAX_SUGGESTIONS
                ? matches.subList(0, MAX_SUGGESTIONS)
                : matches;

        log.debug("Found {} total matches for prefix '{}', returning top {}",
                matches.size(), normalizedPrefix, topResults.size());

        return topResults;
    }

    // ============================================================
    // PRIVATE HELPERS
    // ============================================================

    /**
     * Navigates from root to the node representing the last character
     * of the given prefix.
     *
     * @param prefix the normalized prefix string
     * @return the TrieNode at the end of the prefix path, or null if not found
     */
    private TrieNode navigateToNode(String prefix) {
        TrieNode current = root;
        for (char ch : prefix.toCharArray()) {
            current = current.getChild(ch);
            if (current == null) {
                return null; // Path doesn't exist
            }
        }
        return current;
    }

    /**
     * Depth-First Search to collect all complete queries in the subtree
     * rooted at the given node.
     *
     * Traversal order doesn't matter since we sort by count afterward.
     *
     * @param node    the current node in the DFS
     * @param results accumulator list for found QueryData objects
     */
    private void collectAllWords(TrieNode node, List<QueryData> results) {
        // If this node marks a complete query, add it to results
        if (node.isEndOfWord()) {
            results.add(new QueryData(node.getQuery(), node.getCount()));
        }

        // Recurse into all children
        for (TrieNode child : node.getChildren().values()) {
            collectAllWords(child, results);
        }
    }

    // ============================================================
    // UTILITY
    // ============================================================

    /**
     * Returns the total number of unique queries stored in the Trie.
     * Used for startup logging and health checks.
     *
     * @return count of end-of-word nodes
     */
    public int size() {
        return countWords(root);
    }

    /**
     * Recursively counts all end-of-word nodes.
     */
    private int countWords(TrieNode node) {
        int count = node.isEndOfWord() ? 1 : 0;
        for (TrieNode child : node.getChildren().values()) {
            count += countWords(child);
        }
        return count;
    }
}

package com.typeahead.trie;

import java.util.HashMap;
import java.util.Map;

/**
 * A single node in the Trie data structure.
 *
 * Structure:
 *   - Each node has a map of child nodes keyed by character.
 *   - Leaf nodes (where isEndOfWord == true) store the full query
 *     and its popularity count.
 *
 * Memory layout example for "app" and "apple":
 *
 *   root -> 'a' -> 'p' -> 'p' (end: "app", count: 100)
 *                           |
 *                           'l' -> 'e' (end: "apple", count: 200)
 *
 * Thread safety: Not inherently thread-safe. Synchronization is
 * handled at the Trie level for insert operations.
 */
public class TrieNode {

    /**
     * Map of child characters to their corresponding TrieNodes.
     * Using HashMap for O(1) average-case lookups.
     * Key: single character from the query string
     * Value: child TrieNode for that character
     */
    private final Map<Character, TrieNode> children;

    /**
     * Indicates whether this node marks the end of a complete query.
     * Only nodes where isEndOfWord == true contain valid query data.
     */
    private boolean isEndOfWord;

    /**
     * The full query string (stored only at end-of-word nodes).
     * Stored lowercase for case-insensitive matching.
     */
    private String query;

    /**
     * The popularity count for this query.
     * Only meaningful when isEndOfWord == true.
     */
    private long count;

    /**
     * Constructs a new empty TrieNode.
     * Children map starts empty; no query data is set.
     */
    public TrieNode() {
        this.children = new HashMap<>();
        this.isEndOfWord = false;
        this.query = null;
        this.count = 0;
    }

    // ============================================================
    // Child node operations
    // ============================================================

    /**
     * Returns the child node for the given character, or null if absent.
     *
     * @param ch the character to look up
     * @return the child TrieNode, or null
     */
    public TrieNode getChild(char ch) {
        return children.get(ch);
    }

    /**
     * Returns true if a child node exists for the given character.
     *
     * @param ch the character to check
     * @return true if child exists
     */
    public boolean hasChild(char ch) {
        return children.containsKey(ch);
    }

    /**
     * Adds or replaces the child node for the given character.
     *
     * @param ch   the character key
     * @param node the child TrieNode to store
     */
    public void putChild(char ch, TrieNode node) {
        children.put(ch, node);
    }

    /**
     * Returns the complete map of children.
     * Used by DFS traversal in Trie.getSuggestions().
     *
     * @return unmodifiable view would be safer, but kept mutable for simplicity in Phase 1
     */
    public Map<Character, TrieNode> getChildren() {
        return children;
    }

    // ============================================================
    // End-of-word data accessors
    // ============================================================

    public boolean isEndOfWord() {
        return isEndOfWord;
    }

    public void setEndOfWord(boolean endOfWord) {
        this.isEndOfWord = endOfWord;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}

package com.typeahead.model;

/**
 * Represents a single search query with its popularity count.
 *
 * This is the core data transfer object returned by the suggestion
 * endpoint and stored within the Trie leaf nodes.
 *
 * Using a Java 21 record for immutability and compact syntax.
 * The query field stores the original (lowercased) search term.
 * The count field tracks how many times this query has been searched.
 */
public record QueryData(

        /** The search query string (always stored lowercase) */
        String query,

        /** Popularity count — higher means more frequently searched */
        long count
) {

    /**
     * Creates a new QueryData with an incremented count.
     * Records are immutable, so we return a new instance.
     *
     * @return new QueryData with count + 1
     */
    public QueryData incrementCount() {
        return new QueryData(this.query, this.count + 1);
    }
}

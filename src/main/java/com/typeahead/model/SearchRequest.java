package com.typeahead.model;

/**
 * Request body for the POST /search endpoint.
 *
 * Example JSON:
 * {
 *   "query": "iphone"
 * }
 *
 * Using a record for clean deserialization by Jackson.
 */
public record SearchRequest(

        /** The search query submitted by the user */
        String query
) {
}

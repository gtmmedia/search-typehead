package com.typeahead.model;

import java.util.List;

/**
 * Response wrapper for the GET /suggest endpoint.
 *
 * Example JSON:
 * {
 *   "suggestions": [
 *     { "query": "iphone 15", "count": 18500 },
 *     { "query": "iphone 14", "count": 12000 }
 *   ]
 * }
 */
public record SuggestResponse(

        /** Ordered list of query suggestions (descending by count) */
        List<QueryData> suggestions
) {
}

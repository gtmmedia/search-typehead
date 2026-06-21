package com.typeahead.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "search_queries")
public class SearchQuery {

    @Id
    private String queryText;

    private long popularityCount;

    private long lastUpdated;

    // No-arg constructor required by JPA
    public SearchQuery() {
    }

    public SearchQuery(String queryText, long popularityCount, long lastUpdated) {
        this.queryText = queryText;
        this.popularityCount = popularityCount;
        this.lastUpdated = lastUpdated;
    }

    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    public long getPopularityCount() {
        return popularityCount;
    }

    public void setPopularityCount(long popularityCount) {
        this.popularityCount = popularityCount;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}

package com.typeahead.model;

public class QueryStats {
    private String query;
    private long totalCount;
    private double recentCount;
    private long lastUpdated;

    public QueryStats(String query) {
        this.query = query;
        this.totalCount = 0;
        this.recentCount = 0.0;
        this.lastUpdated = System.currentTimeMillis();
    }

    public synchronized void addCount(int count) {
        this.totalCount += count;
        this.recentCount += count;
        this.lastUpdated = System.currentTimeMillis();
    }

    public synchronized void decay(double factor) {
        this.recentCount *= factor;
    }

    public String getQuery() {
        return query;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public double getRecentCount() {
        return recentCount;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }
}

package com.typeahead.service;

import com.typeahead.model.QueryStats;
import com.typeahead.model.TrendingResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class TrendingService {

    private final ConcurrentHashMap<String, QueryStats> statsMap = new ConcurrentHashMap<>();

    public void update(String query, int count) {
        statsMap.computeIfAbsent(query, QueryStats::new).addCount(count);
    }

    /**
     * Decays recent counts every 1 minute to prevent permanent boosting
     * and support recency.
     */
    @Scheduled(fixedRate = 60000)
    public void decayRecentCounts() {
        for (QueryStats stats : statsMap.values()) {
            stats.decay(0.5); // Halve the recent count
        }
    }

    public List<TrendingResponse> getTrending(String mode) {
        if ("trending".equalsIgnoreCase(mode)) {
            return getTrendingRanking();
        }
        return getHistoricalRanking();
    }

    private List<TrendingResponse> getHistoricalRanking() {
        return statsMap.values().stream()
                .map(stats -> new TrendingResponse(stats.getQuery(), (double) stats.getTotalCount()))
                .sorted(Comparator.comparingDouble(TrendingResponse::score).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }

    private List<TrendingResponse> getTrendingRanking() {
        long maxTotalCount = statsMap.values().stream().mapToLong(QueryStats::getTotalCount).max().orElse(1);
        double maxRecentCount = statsMap.values().stream().mapToDouble(QueryStats::getRecentCount).max().orElse(1.0);

        long finalMaxTotal = Math.max(maxTotalCount, 1);
        double finalMaxRecent = Math.max(maxRecentCount, 1.0);

        return statsMap.values().stream()
                .map(stats -> {
                    double normalizedTotal = (double) stats.getTotalCount() / finalMaxTotal;
                    double normalizedRecent = stats.getRecentCount() / finalMaxRecent;
                    double score = 0.7 * normalizedTotal + 0.3 * normalizedRecent;
                    // Rounding to 2 decimal places as in the example requirement
                    score = Math.round(score * 100.0) / 100.0;
                    return new TrendingResponse(stats.getQuery(), score);
                })
                .sorted(Comparator.comparingDouble(TrendingResponse::score).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }
}

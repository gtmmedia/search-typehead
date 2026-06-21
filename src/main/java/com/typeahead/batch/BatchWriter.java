package com.typeahead.batch;

import com.typeahead.model.SearchEvent;
import com.typeahead.model.SearchQuery;
import com.typeahead.repository.SearchQueryRepository;
import com.typeahead.service.MetricsService;
import com.typeahead.service.SearchService;
import com.typeahead.service.TrendingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BatchWriter {

    private static final Logger log = LoggerFactory.getLogger(BatchWriter.class);
    private static final int BATCH_SIZE_THRESHOLD = 100;

    private final SearchQueue searchQueue;
    private final SearchService searchService;
    private final TrendingService trendingService;
    private final MetricsService metricsService;
    private final SearchQueryRepository searchQueryRepository;

    public BatchWriter(SearchQueue searchQueue, SearchService searchService, TrendingService trendingService, MetricsService metricsService, SearchQueryRepository searchQueryRepository) {
        this.searchQueue = searchQueue;
        this.searchService = searchService;
        this.trendingService = trendingService;
        this.metricsService = metricsService;
        this.searchQueryRepository = searchQueryRepository;
    }

    /**
     * Checks if the queue has reached the threshold. If so, flushes it.
     */
    public void checkAndFlush() {
        if (searchQueue.size() >= BATCH_SIZE_THRESHOLD) {
            flush();
        }
    }

    /**
     * Periodically flushes the queue every 5 seconds.
     */
    @Scheduled(fixedDelay = 5000)
    public void periodicFlush() {
        if (searchQueue.size() > 0) {
            flush();
        }
    }

    private synchronized void flush() {
        List<SearchEvent> events = searchQueue.drainAll();
        if (events.isEmpty()) {
            return;
        }

        int rawWritesCount = events.size();

        // Aggregation: Count occurrences of each query in the batch
        Map<String, Integer> aggregatedCounts = events.stream()
                .collect(Collectors.toMap(
                        SearchEvent::query,
                        e -> 1,
                        Integer::sum
                ));

        int batchedWritesCount = aggregatedCounts.size();

        log.info("Flushing batch: {} raw events aggregated into {} unique queries", rawWritesCount, batchedWritesCount);

        // Batch fetch existing entries from database for all queries in the aggregated map
        List<SearchQuery> existingDbQueries = searchQueryRepository.findAllById(aggregatedCounts.keySet());
        Map<String, SearchQuery> existingDbMap = existingDbQueries.stream()
                .collect(Collectors.toMap(SearchQuery::getQueryText, q -> q));

        List<SearchQuery> dbQueriesToSave = new ArrayList<>();

        // Process each unique query
        for (Map.Entry<String, Integer> entry : aggregatedCounts.entrySet()) {
            String query = entry.getKey();
            int count = entry.getValue();

            // Submit to Trie
            searchService.submitSearch(query, count);

            // Update Trending statistics
            trendingService.update(query, count);

            // Prepare database updates
            SearchQuery sq = existingDbMap.get(query);
            if (sq != null) {
                sq.setPopularityCount(sq.getPopularityCount() + count);
                sq.setLastUpdated(System.currentTimeMillis());
            } else {
                sq = new SearchQuery(query, count, System.currentTimeMillis());
            }
            dbQueriesToSave.add(sq);
        }

        // Batch save database updates
        try {
            searchQueryRepository.saveAll(dbQueriesToSave);
            log.info("Persisted batch updates to the database: {} records saved.", dbQueriesToSave.size());
        } catch (Exception e) {
            log.error("Failed to persist query batch updates to database", e);
        }

        // Record metrics
        metricsService.recordBatchedWrites(batchedWritesCount);
    }
}
}

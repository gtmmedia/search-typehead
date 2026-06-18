package com.typeahead.batch;

import com.typeahead.model.SearchEvent;
import com.typeahead.service.MetricsService;
import com.typeahead.service.SearchService;
import com.typeahead.service.TrendingService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

public class BatchWriterTest {

    @Test
    public void testBatchFlushAggregatesQueries() {
        SearchQueue queue = new SearchQueue();
        SearchService searchService = Mockito.mock(SearchService.class);
        TrendingService trendingService = Mockito.mock(TrendingService.class);
        MetricsService metricsService = Mockito.mock(MetricsService.class);

        BatchWriter writer = new BatchWriter(queue, searchService, trendingService, metricsService);

        queue.enqueue(new SearchEvent("iphone", 1));
        queue.enqueue(new SearchEvent("iphone", 2));
        queue.enqueue(new SearchEvent("iphone", 3));
        queue.enqueue(new SearchEvent("java", 4));

        writer.periodicFlush();

        // Verify aggregation
        verify(searchService).submitSearch(eq("iphone"), eq(3));
        verify(searchService).submitSearch(eq("java"), eq(1));
        
        verify(trendingService).update(eq("iphone"), eq(3));
        verify(trendingService).update(eq("java"), eq(1));
        
        verify(metricsService).recordBatchedWrites(2); // 2 unique queries
    }
}

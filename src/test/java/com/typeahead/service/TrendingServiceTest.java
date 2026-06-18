package com.typeahead.service;

import com.typeahead.model.TrendingResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TrendingServiceTest {

    @Test
    public void testTrendingRanking() {
        TrendingService trendingService = new TrendingService();
        
        // Add queries
        trendingService.update("java", 100);
        trendingService.update("python", 50);
        
        // Decay to simulate time passing
        trendingService.decayRecentCounts();
        
        // Add a recent query
        trendingService.update("rust", 10);
        
        List<TrendingResponse> historical = trendingService.getTrending("historical");
        assertEquals("java", historical.get(0).query());
        assertEquals("python", historical.get(1).query());
        assertEquals("rust", historical.get(2).query());
        
        List<TrendingResponse> trending = trendingService.getTrending("trending");
        // java: total=100(norm=1), recent=50(norm=1) -> score=1.0
        // python: total=50(norm=0.5), recent=25(norm=0.5) -> score=0.5
        // rust: total=10(norm=0.1), recent=10(norm=0.2) -> score=0.07+0.06=0.13
        
        assertEquals("java", trending.get(0).query());
        assertEquals("python", trending.get(1).query());
        assertEquals("rust", trending.get(2).query());
        assertTrue(trending.get(0).score() > trending.get(1).score());
    }
}

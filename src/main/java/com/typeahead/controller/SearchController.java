package com.typeahead.controller;

import com.typeahead.cache.CacheCluster;
import com.typeahead.cache.CacheEntry;
import com.typeahead.cache.CacheNode;
import com.typeahead.model.*;
import com.typeahead.service.MetricsService;
import com.typeahead.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.typeahead.batch.BatchWriter;
import com.typeahead.batch.SearchQueue;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class SearchController {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    private final SearchService searchService;
    private final MetricsService metricsService;
    private final CacheCluster cacheCluster;
    private final SearchQueue searchQueue;
    private final BatchWriter batchWriter;

    public SearchController(SearchService searchService, MetricsService metricsService, CacheCluster cacheCluster, SearchQueue searchQueue, BatchWriter batchWriter) {
        this.searchService = searchService;
        this.metricsService = metricsService;
        this.cacheCluster = cacheCluster;
        this.searchQueue = searchQueue;
        this.batchWriter = batchWriter;
    }

    @GetMapping("/suggest")
    public ResponseEntity<SuggestResponse> suggest(
            @RequestParam(value = "q", defaultValue = "") String prefix) {

        List<QueryData> suggestions = searchService.getSuggestions(prefix);
        return ResponseEntity.ok(new SuggestResponse(suggestions));
    }

    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestBody SearchRequest request) {

        if (request.query() == null || request.query().isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", "Bad Request", "message", "Query cannot be null or empty"));
        }

        String normalizedQuery = request.query().toLowerCase().trim();
        searchQueue.enqueue(new SearchEvent(normalizedQuery, System.currentTimeMillis()));
        metricsService.recordRawWrite();
        batchWriter.checkAndFlush();

        return ResponseEntity.accepted().body(Map.of(
                "query", normalizedQuery,
                "message", "Search request accepted for batch processing"
        ));
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> metrics() {

        Map<String, Object> metricsData = Map.of(
                "searchCount", metricsService.getSearchCount(),
                "suggestionCount", metricsService.getSuggestionCount(),
                "averageLatencyMs", String.format("%.3f", metricsService.getAverageLatencyMs()),
                "p50LatencyMs", String.format("%.3f", metricsService.getP50LatencyMs()),
                "p95LatencyMs", String.format("%.3f", metricsService.getP95LatencyMs()),
                "totalRequests", metricsService.getTotalRequests(),
                "cacheHits", metricsService.getCacheHits(),
                "cacheMisses", metricsService.getCacheMisses(),
                "cacheHitRatio", String.format("%.2f", metricsService.getCacheHitRatio()),
                "cacheMissRate", String.format("%.2f", 1.0 - metricsService.getCacheHitRatio())
        );

        return ResponseEntity.ok(metricsData);
    }
    
    @GetMapping("/cache/debug")
    public ResponseEntity<Map<String, Object>> cacheDebug(
            @RequestParam(value = "prefix") String prefix) {
            
        String normalizedPrefix = prefix.toLowerCase().trim();
        int hash = cacheCluster.getHashForPrefix(normalizedPrefix);
        CacheNode assignedNode = cacheCluster.getNodeForPrefix(normalizedPrefix);
        CacheEntry entry = cacheCluster.get(normalizedPrefix);
        
        boolean cacheHit = (entry != null);
        long ttlRemaining = cacheHit ? entry.getTtlRemainingSeconds() : 0;
        
        return ResponseEntity.ok(Map.of(
                "prefix", normalizedPrefix,
                "hash", hash,
                "assignedNode", assignedNode != null ? assignedNode.getNodeId() : "None",
                "cacheHit", cacheHit,
                "ttlRemaining", ttlRemaining
        ));
    }
    
    @PostMapping("/cache/nodes")
    public ResponseEntity<Map<String, Object>> addNode(@RequestParam String nodeId) {
        cacheCluster.addNode(nodeId);
        return ResponseEntity.ok(Map.of(
                "message", "Node added",
                "nodeId", nodeId,
                "activeNodes", cacheCluster.getActiveNodeIds()
        ));
    }

    @DeleteMapping("/cache/nodes")
    public ResponseEntity<Map<String, Object>> removeNode(@RequestParam String nodeId) {
        cacheCluster.removeNode(nodeId);
        return ResponseEntity.ok(Map.of(
                "message", "Node removed",
                "nodeId", nodeId,
                "activeNodes", cacheCluster.getActiveNodeIds()
        ));
    }
}

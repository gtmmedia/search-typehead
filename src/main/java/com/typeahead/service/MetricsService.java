package com.typeahead.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Arrays;

/**
 * Tracks operational metrics for the Search Typeahead system.
 */
@Service
public class MetricsService {

    private static final Logger log = LoggerFactory.getLogger(MetricsService.class);

    private final LongAdder searchCount = new LongAdder();
    private final LongAdder suggestionCount = new LongAdder();
    private final AtomicLong totalLatencyNanos = new AtomicLong(0);
    private final LongAdder totalRequests = new LongAdder();
    
    private final LongAdder cacheHits = new LongAdder();
    private final LongAdder cacheMisses = new LongAdder();
    
    private final LongAdder rawWrites = new LongAdder();
    private final LongAdder batchedWrites = new LongAdder();

    private static final int MAX_LATENCY_SAMPLES = 10000;
    private final long[] latencySamples = new long[MAX_LATENCY_SAMPLES];
    private final AtomicInteger latencyIndex = new AtomicInteger(0);
    private final AtomicInteger latencyCount = new AtomicInteger(0);

    public void recordSearch() {
        searchCount.increment();
    }

    public void recordSuggestion() {
        suggestionCount.increment();
    }

    public void recordLatency(long startNanos) {
        long elapsed = System.nanoTime() - startNanos;
        totalLatencyNanos.addAndGet(elapsed);
        totalRequests.increment();
        
        long elapsedMs = elapsed / 1_000_000;
        int idx = latencyIndex.getAndIncrement() % MAX_LATENCY_SAMPLES;
        if (idx < 0) {
            // handle overflow safely
            latencyIndex.set(0);
            idx = 0;
        }
        latencySamples[idx] = elapsedMs;
        
        int currentCount = latencyCount.get();
        if (currentCount < MAX_LATENCY_SAMPLES) {
            latencyCount.incrementAndGet();
        }
    }

    public void recordCacheHit() {
        cacheHits.increment();
    }

    public void recordCacheMiss() {
        cacheMisses.increment();
    }

    public long getSearchCount() {
        return searchCount.sum();
    }

    public long getSuggestionCount() {
        return suggestionCount.sum();
    }

    public double getAverageLatencyMs() {
        long total = totalRequests.sum();
        if (total == 0) {
            return 0.0;
        }
        return (totalLatencyNanos.get() / (double) total) / 1_000_000.0;
    }

    public long getTotalRequests() {
        return totalRequests.sum();
    }
    
    public long getCacheHits() {
        return cacheHits.sum();
    }
    
    public long getCacheMisses() {
        return cacheMisses.sum();
    }
    
    public double getCacheHitRatio() {
        long hits = getCacheHits();
        long misses = getCacheMisses();
        long total = hits + misses;
        if (total == 0) {
            return 0.0;
        }
        return (double) hits / total;
    }

    public void recordRawWrite() {
        rawWrites.increment();
    }

    public void recordBatchedWrites(int count) {
        batchedWrites.add(count);
    }

    public long getRawWrites() {
        return rawWrites.sum();
    }

    public long getBatchedWrites() {
        return batchedWrites.sum();
    }

    public double getWriteReduction() {
        long raw = getRawWrites();
        long batched = getBatchedWrites();
        if (raw == 0) return 0.0;
        return (1.0 - ((double) batched / raw)) * 100.0;
    }

    private double getPercentile(double percentile) {
        int count = Math.min(latencyCount.get(), MAX_LATENCY_SAMPLES);
        if (count == 0) return 0.0;

        long[] currentSamples = new long[count];
        System.arraycopy(latencySamples, 0, currentSamples, 0, count);
        Arrays.sort(currentSamples);

        int index = (int) Math.ceil(percentile / 100.0 * count) - 1;
        if (index < 0) index = 0;
        if (index >= count) index = count - 1;

        return currentSamples[index];
    }

    public double getP50LatencyMs() {
        return getPercentile(50.0);
    }

    public double getP95LatencyMs() {
        return getPercentile(95.0);
    }
}

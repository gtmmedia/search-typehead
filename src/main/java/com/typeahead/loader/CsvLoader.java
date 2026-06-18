package com.typeahead.loader;

import com.typeahead.config.DatasetConfig;
import com.typeahead.trie.Trie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Loads the initial search query dataset from a CSV file into the Trie
 * on application startup.
 *
 * Implements CommandLineRunner so it executes after the Spring context
 * is fully initialized but before the application starts accepting requests.
 *
 * CSV format (with header row):
 *   query,count
 *   iphone 15,18500
 *   samsung galaxy s24,15300
 *
 * Error handling:
 *   - Malformed rows are logged and skipped (fail-safe).
 *   - Invalid count values are logged and skipped.
 *   - Missing CSV file causes a startup failure (fail-fast).
 */
@Component
public class CsvLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CsvLoader.class);

    private final Trie trie;
    private final DatasetConfig datasetConfig;
    private final ResourceLoader resourceLoader;

    /**
     * Constructor injection — preferred over field injection for testability.
     *
     * @param trie           the shared Trie instance
     * @param datasetConfig  configuration holding the CSV file path
     * @param resourceLoader Spring's resource loader (resolves classpath:/file: URIs)
     */
    public CsvLoader(Trie trie, DatasetConfig datasetConfig, ResourceLoader resourceLoader) {
        this.trie = trie;
        this.datasetConfig = datasetConfig;
        this.resourceLoader = resourceLoader;
    }

    /**
     * Executes on startup. Reads the CSV file line-by-line and inserts
     * each query into the Trie.
     *
     * Performance: For 50K queries, this completes in < 500ms on modern hardware.
     * The Trie insert is O(L) per query where L = query length.
     *
     * @param args command-line arguments (unused)
     * @throws Exception if the CSV file cannot be found or read
     */
    @Override
    public void run(String... args) throws Exception {
        String csvPath = datasetConfig.getCsvPath();
        log.info("Loading search dataset from: {}", csvPath);

        long startTime = System.currentTimeMillis();
        int successCount = 0;
        int errorCount = 0;

        // Use Spring's ResourceLoader to resolve classpath: and file: prefixes
        var resource = resourceLoader.getResource(csvPath);

        if (!resource.exists()) {
            log.error("Dataset CSV file not found: {}", csvPath);
            throw new IllegalStateException("Dataset CSV file not found: " + csvPath);
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                // Skip the header row
                if (isFirstLine) {
                    isFirstLine = false;
                    log.debug("Skipping CSV header: {}", line);
                    continue;
                }

                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    // Parse CSV line: query,count
                    // Using lastIndexOf to handle queries that may contain commas
                    int lastCommaIndex = line.lastIndexOf(',');

                    if (lastCommaIndex == -1 || lastCommaIndex == 0) {
                        log.warn("Malformed CSV line (no comma or empty query): '{}'", line);
                        errorCount++;
                        continue;
                    }

                    String query = line.substring(0, lastCommaIndex).trim();
                    String countStr = line.substring(lastCommaIndex + 1).trim();

                    // Validate query is not empty
                    if (query.isEmpty()) {
                        log.warn("Empty query in CSV line: '{}'", line);
                        errorCount++;
                        continue;
                    }

                    // Parse count — must be a positive number
                    long count = Long.parseLong(countStr);
                    if (count < 0) {
                        log.warn("Negative count in CSV line: '{}'", line);
                        errorCount++;
                        continue;
                    }

                    // Insert into the Trie
                    trie.insert(query, count);
                    successCount++;

                } catch (NumberFormatException e) {
                    log.warn("Invalid count format in CSV line: '{}' — {}", line, e.getMessage());
                    errorCount++;
                }
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Dataset loading complete: {} queries loaded, {} errors, {} ms elapsed. Trie size: {}",
                successCount, errorCount, duration, trie.size());
    }
}

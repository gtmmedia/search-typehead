package com.typeahead.loader;

import com.typeahead.config.DatasetConfig;
import com.typeahead.model.SearchQuery;
import com.typeahead.repository.SearchQueryRepository;
import com.typeahead.trie.Trie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the initial search query dataset into the Trie on application startup.
 * If the database already contains records, they are loaded into the Trie.
 * Otherwise, it loads the records from a CSV file, populates the database,
 * and inserts them into the Trie.
 */
@Component
public class CsvLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CsvLoader.class);

    private final Trie trie;
    private final DatasetConfig datasetConfig;
    private final ResourceLoader resourceLoader;
    private final SearchQueryRepository searchQueryRepository;

    public CsvLoader(Trie trie, DatasetConfig datasetConfig, ResourceLoader resourceLoader, SearchQueryRepository searchQueryRepository) {
        this.trie = trie;
        this.datasetConfig = datasetConfig;
        this.resourceLoader = resourceLoader;
        this.searchQueryRepository = searchQueryRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        long startTime = System.currentTimeMillis();

        // 1. Check if database has records
        long dbCount = searchQueryRepository.count();
        if (dbCount > 0) {
            log.info("Found {} existing search queries in database. Hydrating Trie...", dbCount);
            List<SearchQuery> dbQueries = searchQueryRepository.findAll();
            int loadCount = 0;
            for (SearchQuery sq : dbQueries) {
                trie.insert(sq.getQueryText(), sq.getPopularityCount());
                loadCount++;
            }
            long duration = System.currentTimeMillis() - startTime;
            log.info("Trie hydration complete: {} queries loaded from database, {} ms elapsed. Trie size: {}",
                    loadCount, duration, trie.size());
            return;
        }

        // 2. Fallback to CSV if database is empty
        String csvPath = datasetConfig.getCsvPath();
        log.info("Database is empty. Loading search dataset from CSV: {}", csvPath);

        int successCount = 0;
        int errorCount = 0;

        var resource = resourceLoader.getResource(csvPath);

        if (!resource.exists()) {
            log.error("Dataset CSV file not found: {}", csvPath);
            throw new IllegalStateException("Dataset CSV file not found: " + csvPath);
        }

        List<SearchQuery> dbQueriesToSave = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    int lastCommaIndex = line.lastIndexOf(',');

                    if (lastCommaIndex == -1 || lastCommaIndex == 0) {
                        log.warn("Malformed CSV line (no comma or empty query): '{}'", line);
                        errorCount++;
                        continue;
                    }

                    String query = line.substring(0, lastCommaIndex).trim();
                    String countStr = line.substring(lastCommaIndex + 1).trim();

                    if (query.isEmpty()) {
                        log.warn("Empty query in CSV line: '{}'", line);
                        errorCount++;
                        continue;
                    }

                    long count = Long.parseLong(countStr);
                    if (count < 0) {
                        log.warn("Negative count in CSV line: '{}'", line);
                        errorCount++;
                        continue;
                    }

                    // Insert into the Trie
                    trie.insert(query, count);
                    
                    // Add to batch save list
                    dbQueriesToSave.add(new SearchQuery(query, count, System.currentTimeMillis()));
                    successCount++;

                } catch (NumberFormatException e) {
                    log.warn("Invalid count format in CSV line: '{}' — {}", line, e.getMessage());
                    errorCount++;
                }
            }
        }

        // Batch save to the H2 database
        if (!dbQueriesToSave.isEmpty()) {
            searchQueryRepository.saveAll(dbQueriesToSave);
            log.info("Saved {} initial queries from CSV into the database.", dbQueriesToSave.size());
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Dataset CSV loading complete: {} queries loaded, {} errors, {} ms elapsed. Trie size: {}",
                successCount, errorCount, duration, trie.size());
    }
}

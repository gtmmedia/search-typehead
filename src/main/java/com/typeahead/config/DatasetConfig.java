package com.typeahead.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the search dataset.
 *
 * Binds to the `dataset` prefix in application.yml:
 *
 *   dataset:
 *     csv-path: classpath:queries.csv
 *
 * This allows the CSV file path to be externalized and changed
 * without modifying Java code (e.g., via environment variables).
 */
@Configuration
@ConfigurationProperties(prefix = "dataset")
@Getter
@Setter
public class DatasetConfig {

    /**
     * Path to the CSV file containing initial search queries.
     * Supports classpath: and file: prefixes.
     * Default: classpath:queries.csv
     */
    private String csvPath = "classpath:queries.csv";
}

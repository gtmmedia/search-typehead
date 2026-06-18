package com.typeahead.controller;

import com.typeahead.service.MetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@RequestMapping("/batch")
@CrossOrigin(origins = "http://localhost:5173")
public class BatchController {

    private final MetricsService metricsService;

    public BatchController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getBatchStats() {
        return ResponseEntity.ok(Map.of(
                "rawWrites", metricsService.getRawWrites(),
                "batchedWrites", metricsService.getBatchedWrites(),
                "writeReduction", metricsService.getWriteReduction()
        ));
    }
}

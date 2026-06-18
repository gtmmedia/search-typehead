package com.typeahead.controller;

import com.typeahead.model.TrendingResponse;
import com.typeahead.service.TrendingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class TrendingController {

    private final TrendingService trendingService;

    public TrendingController(TrendingService trendingService) {
        this.trendingService = trendingService;
    }

    @GetMapping("/trending")
    public ResponseEntity<List<TrendingResponse>> getTrending(
            @RequestParam(value = "mode", defaultValue = "trending") String mode) {
        
        List<TrendingResponse> trending = trendingService.getTrending(mode);
        return ResponseEntity.ok(trending);
    }
}

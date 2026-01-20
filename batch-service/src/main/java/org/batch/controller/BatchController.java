package org.batch.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.batch.scheduler.AccessLogJobScheduler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for manual batch job triggering
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
public class BatchController {

    private final AccessLogJobScheduler scheduler;

    /**
     * Manually trigger the batch job
     * POST /api/v1/batch/trigger
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, String>> triggerBatchJob() {
        log.info("Manual batch job trigger requested via API");
        
        try {
            scheduler.runManualJob();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Batch job triggered successfully"
            ));
        } catch (Exception e) {
            log.error("Failed to trigger batch job: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Health check endpoint
     * GET /api/v1/batch/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "batch-service"
            ));
    }
}

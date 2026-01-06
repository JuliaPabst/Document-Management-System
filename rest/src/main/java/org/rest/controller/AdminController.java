package org.rest.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rest.dto.DocumentIndexDto;
import org.rest.model.FileMetadata;
import org.rest.service.FileMetadataService;
import org.rest.service.MessageProducerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin controller for maintenance operations (reindexing)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final FileMetadataService fileMetadataService;
    private final MessageProducerService messageProducerService;

    // Reindex all documents from PostgreSQL to Elasticsearch
    // Fetch all documents and send them to search-indexing-queue
    // Return Response with reindex statistics
    @PostMapping("/reindex")
    public ResponseEntity<Map<String, Object>> reindexAllDocuments() {
        log.info("Starting reindex operation for all documents");
        
        try {
            // Get all documents from PostgreSQL
            List<FileMetadata> allDocuments = fileMetadataService.getAllFileMetadata();
            log.info("Found {} documents to reindex", allDocuments.size());
            
            int successCount = 0;
            int failureCount = 0;
            
            // Send each document to search-indexing-queue for indexing
            for (FileMetadata metadata : allDocuments) {
                try {
                    DocumentIndexDto indexDto = DocumentIndexDto.builder()
                            .documentId(metadata.getId())
                            .filename(metadata.getFilename())
                            .author(metadata.getAuthor())
                            .fileType(metadata.getFileType())
                            .size(metadata.getSize())
                            .objectKey(metadata.getObjectKey())
                            .uploadTime(metadata.getUploadTime().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())
                            .extractedText(null)  // not stored in FileMetadata yet
                            .summary(metadata.getSummary())
                            .build();
                    
                    // Send DocumentIndexDto directly (same as from workers)
                    messageProducerService.sendDocumentForIndexing(indexDto);
                    successCount++;
                    log.debug("Sent document {} for reindexing", metadata.getId());
                } catch (Exception e) {
                    failureCount++;
                    log.error("Failed to send document {} for reindexing: {}", metadata.getId(), e.getMessage());
                }
            }
            
            log.info("Reindex operation completed: {} successful, {} failed", successCount, failureCount);
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalDocuments", allDocuments.size());
            response.put("successCount", successCount);
            response.put("failureCount", failureCount);
            response.put("message", String.format("Reindex completed: %d documents sent for indexing", successCount));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Reindex operation failed: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Reindex operation failed");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    // Get reindex status/health check
    @GetMapping("/reindex/status")
    public ResponseEntity<Map<String, Object>> getReindexStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "ready");
        status.put("message", "Reindex endpoint is POST /api/v1/admin/reindex to start reindexing.");
        
        return ResponseEntity.ok(status);
    }
}
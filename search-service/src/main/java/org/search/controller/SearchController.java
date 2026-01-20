package org.search.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.search.dto.SearchRequestDto;
import org.search.dto.SearchResponseDto;
import org.search.service.ElasticsearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * REST controller for document search operations using Elasticsearch.
 * Provides POST/GET search endpoints and document deletion.
 */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final ElasticsearchService elasticsearchService;

    @PostMapping
    public ResponseEntity<SearchResponseDto> search(@Valid @RequestBody SearchRequestDto searchRequest) {
        log.info("Received search request: {}", searchRequest.getQuery());

        try {
            SearchResponseDto response = elasticsearchService.search(searchRequest);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Search failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    public ResponseEntity<SearchResponseDto> searchByQuery(
            @RequestParam String query,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String fileType,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "uploadTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder
    ) {
        log.info("Received GET search request: query={}", query);

        SearchRequestDto searchRequest = SearchRequestDto.builder()
                .query(query)
                .author(author)
                .fileType(fileType)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortOrder(sortOrder)
                .build();

        try {
            SearchResponseDto response = elasticsearchService.search(searchRequest);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Search failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long documentId) {
        log.info("Received delete request for document: {}", documentId);

        try {
            elasticsearchService.deleteDocument(documentId);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            log.error("Delete failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}

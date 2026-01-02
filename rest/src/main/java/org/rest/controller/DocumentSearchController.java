package org.rest.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.rest.dto.SearchRequestDto;
import org.rest.dto.SearchResponseDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class DocumentSearchController {

    private final RestTemplate restTemplate;
    private final String searchServiceUrl;

    public DocumentSearchController(
            RestTemplate restTemplate,
            @Qualifier("searchServiceUrl") String searchServiceUrl) {
        this.restTemplate = restTemplate;
        this.searchServiceUrl = searchServiceUrl;
    }

    @PostMapping("/search")
    public ResponseEntity<SearchResponseDto> searchDocuments(
            @Valid @RequestBody SearchRequestDto searchRequest) {
        
        log.info("Proxying search request to search-service: query='{}', author='{}', fileType='{}'",
                searchRequest.getQuery(),
                searchRequest.getAuthor(),
                searchRequest.getFileType());

        try {
            String searchUrl = searchServiceUrl + "/api/v1/search";
            
            SearchResponseDto response = restTemplate.postForObject(
                    searchUrl,
                    searchRequest,
                    SearchResponseDto.class
            );

            if (response == null) {
                log.error("Search service returned null response");
                throw new RuntimeException("Search service returned empty response");
            }

            log.info("Search completed: {} results found in {}ms",
                    response.getTotalHits(),
                    response.getSearchTimeMs());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error proxying search request to search-service", e);
            throw new RuntimeException("Search service unavailable", e);
        }
    }
}

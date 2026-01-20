package org.search.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Search request DTO with query, filters, pagination and sorting parameters.
 * Supports filtering by author, file type and specific search fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequestDto {

    @NotBlank(message = "Search query cannot be empty")
    private String query;

    private String author;
    private String fileType;
    private String searchField; // all, filename, extractedText, summary

    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 10;

    @Builder.Default
    private String sortBy = "uploadTime";

    @Builder.Default
    private String sortOrder = "desc";
}
package org.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Search response DTO containing search results, pagination metadata and search execution time
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponseDto {
    private List<SearchResultDto> results;
    private Long totalHits;
    private Integer page;
    private Integer size;
    private Integer totalPages;
    private Long searchTimeMs;
}

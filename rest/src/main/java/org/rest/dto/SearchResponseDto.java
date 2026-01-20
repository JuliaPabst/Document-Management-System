package org.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for document search containing results, page info and search metrics
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

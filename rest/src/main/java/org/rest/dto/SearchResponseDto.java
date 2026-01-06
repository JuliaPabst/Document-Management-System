package org.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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

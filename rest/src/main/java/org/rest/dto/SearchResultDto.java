package org.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Individual search result DTO containing document metadata, relevance score and highlighted text
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDto {
    private Long documentId;
    private String filename;
    private String author;
    private String fileType;
    private Long size;
    private String objectKey;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime uploadTime;

    private String summary;
    private Double score;
    private String highlightedText;
}

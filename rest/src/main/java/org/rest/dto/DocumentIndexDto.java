package org.rest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for sending document data to search-service for Elasticsearch indexing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentIndexDto {
    private Long documentId;
    private String filename;
    private String author;
    private String fileType;
    private Long size;
    private String objectKey;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime uploadTime;

    private String extractedText;
    private String summary;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime processedTime;
}
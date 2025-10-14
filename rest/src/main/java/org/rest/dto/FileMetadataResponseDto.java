package org.rest.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Schema(description = "DTO for file metadata response")
public class FileMetadataResponseDto {
    @Schema(description = "ID of the file metadata", example = "1")
    private Long id;

    @Schema(description = "Filename of the document", example = "invoice.pdf")
    private String filename;

    @Schema(description = "Author of the document", example = "John Doe")
    private String author;

    @Schema(description = "Type of the file", example = "PDF")
    private String fileType;

    @Schema(description = "Size of the file in bytes", example = "1024")
    private Long size;

    @Schema(description = "Upload time of the file", example = "2024-06-01T12:00:00")
    private LocalDateTime uploadTime;

    @Schema(description = "Last edited time of the file", example = "2024-06-02T15:30:00")
    private LocalDateTime lastEdited;
}
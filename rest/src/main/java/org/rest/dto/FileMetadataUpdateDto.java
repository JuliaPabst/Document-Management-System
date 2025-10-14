package org.rest.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@Schema(description = "DTO for updating file metadata")
public class FileMetadataUpdateDto {
    @Schema(description = "Filename of the document", example = "updated_invoice.pdf")
    private String filename;

    @Schema(description = "Author of the document", example = "Jane Doe")
    private String author;

    @Schema(description = "Type of the file", example = "PDF")
    private String fileType;

    @Schema(description = "Size of the file in bytes", example = "2048")
    private Long size;
}
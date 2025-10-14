package org.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO for creating file metadata")
public class FileMetadataCreateDto {
    @Schema(description = "Filename of the document", example = "invoice.pdf")
    @NotBlank(message = "Filename is required")
    private String filename;

    @Schema(description = "Author of the document", example = "John Doe")
    @NotBlank(message = "Author is required")
    private String author;

    @Schema(description = "Type of the file", example = "pdf")
    @NotBlank(message = "File type is required")
    private String fileType;

    @Schema(description = "Size of the file in bytes", example = "1024")
    @NotNull(message = "File size is required")
    @Positive(message = "File size must be positive")
    private Long size;
}
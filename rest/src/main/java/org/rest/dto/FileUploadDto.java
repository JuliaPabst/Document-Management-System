package org.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for file upload containing author metadata (filename and type extracted from file)
 */
@Data
@NoArgsConstructor
@Schema(description = "DTO for file upload with metadata")
public class FileUploadDto {

    @Schema(description = "Author of the document", example = "John Doe")
    @NotBlank(message = "Author is required")
    private String author;

    // filename, fileType, and size will be extracted from the uploaded MultipartFile
}
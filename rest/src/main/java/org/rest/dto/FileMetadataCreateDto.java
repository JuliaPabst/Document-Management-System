package org.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadataCreateDto {
    
    @NotBlank(message = "Filename is required")
    private String filename;
    
    @NotBlank(message = "Author is required")
    private String author;
    
    @NotBlank(message = "File type is required")
    private String fileType;
    
    @NotNull(message = "File size is required")
    @Positive(message = "File size must be positive")
    private Long size;
}
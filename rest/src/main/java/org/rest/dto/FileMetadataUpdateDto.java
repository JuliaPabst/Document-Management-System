package org.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadataUpdateDto {
    private String filename;
    private String author;
    private String fileType;
    private Long size;
}
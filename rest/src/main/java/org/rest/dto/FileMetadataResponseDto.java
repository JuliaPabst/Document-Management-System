package org.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadataResponseDto {
    private Long id;
    private String filename;
    private String author;
    private String fileType;
    private Long size;
    private LocalDateTime uploadTime;
    private LocalDateTime lastEdited;
}
package org.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileMessageDto {
    private Long id;
    private String filename;
    private String author;
    private String fileType;
    private Long size;
    private Instant uploadTime;
    private String objectKey;
}

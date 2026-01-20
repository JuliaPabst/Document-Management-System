package org.emailingestion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Message DTO sent to RabbitMQ queues for file processing (OCR and GenAI)
 */
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

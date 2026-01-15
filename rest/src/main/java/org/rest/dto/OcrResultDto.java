package org.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for OCR processing results sent from OcrWorker to GenAIWorker
 * Used for message serialization/deserialization in RabbitMQ queues
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcrResultDto {
    private Long documentId;
    private String objectKey;
    private String bucketName;
    private String extractedText;
    private LocalDateTime processedAt;
    private String ocrEngine;
}

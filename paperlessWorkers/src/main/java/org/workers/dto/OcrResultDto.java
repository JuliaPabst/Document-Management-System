package org.workers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 
 * DTO for OCR processing results sent from OcrWorker to GenAIWorker
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
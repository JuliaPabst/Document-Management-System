package org.workers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 
 * DTO for GenAI processing results sent from GenAIWorker back to REST service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenAiResultDto {
	private Long documentId;
	private String objectKey;
	private String extractedText;
	private String summary;
	private LocalDateTime processedAt;
}
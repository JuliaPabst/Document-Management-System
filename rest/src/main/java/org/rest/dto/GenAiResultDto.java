package org.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 
 * DTO for GenAI processing results received from GenAIWorker
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
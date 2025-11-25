package org.rest.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rest.dto.GenAiResultDto;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GenAIResultListener {

	@RabbitListener(queues = "genai-result-queue")
	public void handleGenAiResult(GenAiResultDto result) {
		log.info("REST received GenAI result for document ID: {}, summary length: {} chars",
				result.getDocumentId(), result.getSummary().length());

		// TODO: Save summary to database (add summary field to FileMetadata entity)
		log.info("GenAI Summary for document {}: {}",
				result.getDocumentId(),
				result.getSummary().substring(0, Math.min(100, result.getSummary().length())) + "...");

		log.info("GenAI result processed successfully for document ID: {}", result.getDocumentId());
	}
}
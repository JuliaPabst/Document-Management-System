package org.rest.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rest.dto.DocumentIndexDto;
import org.rest.dto.GenAiResultDto;
import org.rest.model.FileMetadata;
import org.rest.service.FileMetadataService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
@Slf4j
public class GenAIResultListener {

	private final FileMetadataService fileMetadataService;
	private final RabbitTemplate rabbitTemplate;

	@Value("${rabbitmq.queue.search-indexing}")
	private String searchIndexingQueue;

	@RabbitListener(queues = "genai-result-queue")
	public void handleGenAiResult(GenAiResultDto result) {
		log.info("REST received GenAI result for document ID: {}, summary length: {} chars, extractedText length: {} chars",
				result.getDocumentId(), result.getSummary().length(), result.getExtractedText().length());

		// Save summary to database
		try {
			fileMetadataService.updateSummary(result.getDocumentId(), result.getSummary());
			log.info("Summary saved to database for document ID: {}", result.getDocumentId());
		} catch (Exception e) {
			log.error("Failed to save summary for document ID: {}", result.getDocumentId(), e);
			return; // Don't index if DB update fails
		}

		// Send document to search-indexing-queue for Elasticsearch indexing
		try {
			FileMetadata metadata = fileMetadataService.getFileMetadataById(result.getDocumentId());
			
			DocumentIndexDto indexDto = DocumentIndexDto.builder()
					.documentId(metadata.getId())
					.filename(metadata.getFilename())
					.author(metadata.getAuthor())
					.fileType(metadata.getFileType())
					.size(metadata.getSize())
					.objectKey(metadata.getObjectKey())
					.uploadTime(LocalDateTime.ofInstant(metadata.getUploadTime(), ZoneId.systemDefault()))
					.extractedText(result.getExtractedText())
					.summary(result.getSummary())
					.processedTime(result.getProcessedAt())
					.build();

			rabbitTemplate.convertAndSend(searchIndexingQueue, indexDto);
			log.info("Document sent to search-indexing-queue for document ID: {}", result.getDocumentId());
		} catch (Exception e) {
			log.error("Failed to send document to search-indexing-queue for document ID: {}", 
					result.getDocumentId(), e);
		}

		log.info("GenAI Summary for document {}: {}",
				result.getDocumentId(),
				result.getSummary().substring(0, Math.min(100, result.getSummary().length())) + "...");

		log.info("GenAI result processed successfully for document ID: {}", result.getDocumentId());
	}
}
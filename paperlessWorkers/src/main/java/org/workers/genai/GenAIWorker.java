package org.workers.genai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.workers.dto.GenAiResultDto;
import org.workers.dto.OcrResultDto;
import org.workers.service.OpenAIService;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class GenAIWorker {

    private final RabbitTemplate rabbitTemplate;
    private final OpenAIService openAIService;

    @Value("${rabbitmq.queue.genai.result}")
    private String resultQueueName;

    @RabbitListener(queues = "genai-worker-queue")
    public void processGenAiTask(OcrResultDto ocrResult) {
        log.info("GenAI Worker received OCR result for document ID: {}, text length: {} chars", 
                ocrResult.getDocumentId(), ocrResult.getExtractedText().length());

        try {
            // Generate AI summary from OCR text
            log.debug("Generating AI summary for document ID: {} with OpenAI", ocrResult.getDocumentId());
            String summary = openAIService.generateSummary(ocrResult.getExtractedText());
            log.info("GenAI Worker generated summary ({} chars) for document ID: {}", 
                    summary.length(), ocrResult.getDocumentId());

            // Create result DTO
            GenAiResultDto genAiResult = new GenAiResultDto(
                    ocrResult.getDocumentId(),
                    ocrResult.getObjectKey(),
                    summary,
                    LocalDateTime.now()
            );

            // Send result back to REST service for persistence
            log.info("GenAI Worker sending result to result queue for document ID: {}", 
                    ocrResult.getDocumentId());
            rabbitTemplate.convertAndSend(resultQueueName, genAiResult);
            log.info("GenAI Worker successfully sent result to result queue");

        } catch (Exception e) {
            log.error("GenAI Worker failed to process document ID {}: {}", 
                    ocrResult.getDocumentId(), e.getMessage(), e);
        }
    }
}
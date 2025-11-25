package org.workers.ocr;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.workers.dto.FileMessageDto;
import org.workers.dto.OcrResultDto;
import org.workers.service.FileStorage;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class OcrWorker {

    private final RabbitTemplate rabbitTemplate;
    private final FileStorage fileStorage;

    @Value("${rabbitmq.queue.genai}")
    private String genAiQueueName;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @RabbitListener(queues = "ocr-worker-queue")
    public void processOcrTask(FileMessageDto message) {
        log.info("OCR Worker received message for document ID: {}, file: {}", 
                message.getId(), message.getFilename());

        try {
            // Download file from MinIO
            byte[] fileContent = fileStorage.download(message.getObjectKey());
            log.info("OCR Worker downloaded file from MinIO: {} ({} bytes)", 
                    message.getObjectKey(), fileContent.length);

            // Simulate OCR processing (skeleton only, no real implementation)
            String extractedText = performOcrProcessing(fileContent, message.getFilename());
            log.info("OCR Worker extracted {} characters from file: {}", 
                    extractedText.length(), message.getFilename());

            // Create structured result DTO
            OcrResultDto ocrResult = new OcrResultDto(
                    message.getId(),
                    message.getObjectKey(),
                    bucketName,
                    extractedText,
                    LocalDateTime.now(),
                    "Tesseract-Dummy"
            );

            // Send directly to GenAI Queue (Pipeline Pattern)
            log.info("OCR Worker sending result to GenAI Queue for document ID: {}", message.getId());
            rabbitTemplate.convertAndSend(genAiQueueName, ocrResult);
            log.info("OCR Worker successfully sent result to GenAI Queue");

        } catch (Exception e) {
            log.error("OCR Worker failed to process document ID {}: {}", 
                    message.getId(), e.getMessage(), e);
            // TODO: proper error handling and retries
        }
    }

    // Simulated OCR processing
    private String performOcrProcessing(byte[] fileContent, String filename) {
        // TODO: Replace with Tesseract OCR
        return String.format(
                "[OCR Simulated Text]\n" +
                "Document: %s\n" +
                "Size: %d bytes\n" +
                "Extracted at: %s\n" +
                "Content: ...",
                filename,
                fileContent.length,
                LocalDateTime.now()
        );
    }
}

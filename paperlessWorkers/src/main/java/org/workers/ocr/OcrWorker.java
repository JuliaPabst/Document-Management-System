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
import org.workers.service.TesseractOcrService;

import java.time.LocalDateTime;

/**
 * RabbitMQ listener that processes files from MinIO using Tesseract OCR.
 * Extracts text from PDFs and images, then forwards results to GenAI queue.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OcrWorker {

    private final RabbitTemplate rabbitTemplate;
    private final FileStorage fileStorage;
    private final TesseractOcrService tesseractOcrService;

    @Value("${rabbitmq.queue.genai}")
    private String genAiQueueName;

    @Value("${minio.bucket-name}")
    private String bucketName;

    /**
     * Processes file messages: downloads from MinIO, extracts text via OCR, forwards to GenAI
     */
    @RabbitListener(queues = "ocr-worker-queue")
    public void processOcrTask(FileMessageDto message) {
        log.info("OCR Worker received message for document ID: {}, file: {}", 
                message.getId(), message.getFilename());

        try {
            // Download file from MinIO
            byte[] fileContent = fileStorage.download(message.getObjectKey());
            log.info("OCR Worker downloaded file from MinIO: {} ({} bytes)", 
                    message.getObjectKey(), fileContent.length);

            // Perform OCR processing with Tesseract
            String extractedText = performOcrProcessing(fileContent, message.getFilename(), message.getFileType());
            log.info("OCR Worker extracted {} characters from file: {}", 
                    extractedText.length(), message.getFilename());

            // Create structured result DTO
            OcrResultDto ocrResult = new OcrResultDto(
                    message.getId(),
                    message.getObjectKey(),
                    bucketName,
                    extractedText,
                    LocalDateTime.now(),
                    tesseractOcrService.getVersion()
            );

            // Send directly to GenAI Queue (Pipeline Pattern)
            log.info("OCR Worker sending result to GenAI Queue for document ID: {}", message.getId());
            rabbitTemplate.convertAndSend(genAiQueueName, ocrResult);
            log.info("OCR Worker successfully sent result to GenAI Queue");

        } catch (Exception e) {
            log.error("OCR Worker failed to process document ID {}: {}", 
                    message.getId(), e.getMessage(), e);
        }
    }

    // Perform OCR processing based on file type
    private String performOcrProcessing(byte[] fileContent, String filename, String fileType) {
        try {
            if ("PDF".equalsIgnoreCase(fileType)) {
                return tesseractOcrService.extractTextFromPdf(fileContent, filename);
            } else if (isImageFile(fileType)) {
                return tesseractOcrService.extractTextFromImage(fileContent, filename);
            } else {
                log.warn("Unsupported file type for OCR: {}", fileType);
                return "[OCR not supported for file type: " + fileType + "]";
            }
        } catch (Exception e) {
            log.error("OCR processing failed for {}: {}", filename, e.getMessage(), e);
            return "[OCR Error: " + e.getMessage() + "]";
        }
    }

    // Check if file type is a supported image format
    private boolean isImageFile(String fileType) {
        return fileType != null && 
               (fileType.equalsIgnoreCase("PNG") || 
                fileType.equalsIgnoreCase("JPG") || 
                fileType.equalsIgnoreCase("JPEG") || 
                fileType.equalsIgnoreCase("TIFF") ||
                fileType.equalsIgnoreCase("BMP"));
    }
}

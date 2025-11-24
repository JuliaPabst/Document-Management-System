package org.workers.ocr;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.workers.dto.FileMessageDto;
import org.workers.service.FileStorage;

@Component
@RequiredArgsConstructor
@Slf4j
public class OcrWorker {

    private final RabbitTemplate rabbitTemplate;
    private final FileStorage fileStorage;

    @Value("${rabbitmq.queue.ocr.result}")
    private String resultQueueName;

    @RabbitListener(queues = "ocr-worker-queue")
    public void processOcrTask(FileMessageDto message) {
        log.info("OCR Worker received message: {}", message);

        try {
            // Download file from MinIO
            byte[] fileContent = fileStorage.download(message.getObjectKey());
            log.info("OCR Worker downloaded file from MinIO: {} ({} bytes)", 
                    message.getObjectKey(), fileContent.length);

            // Simulate OCR processing (skeleton only - no real implementation) -> TODO
            String result = "Result from OCR Worker - processed file: " + message.getFilename()
                    + " (" + fileContent.length + " bytes)";

            // Send result back to result queue
            log.info("OCR Worker sending result to queue: {}", result);
            rabbitTemplate.convertAndSend(resultQueueName, result);
            log.info("OCR Worker result sent successfully");
        } catch (Exception e) {
            log.error("OCR Worker failed to process file: {}", e.getMessage(), e);
            // TODO: proper error handling
        }
    }
}

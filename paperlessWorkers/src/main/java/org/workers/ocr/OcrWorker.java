package org.workers.ocr;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.workers.dto.FileMessageDto;

@Component
@RequiredArgsConstructor
@Slf4j
public class OcrWorker {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.queue.ocr.result}")
    private String resultQueueName;

    @RabbitListener(queues = "ocr-worker-queue")
    public void processOcrTask(FileMessageDto message) {
        log.info("OCR Worker received message: {}", message);

        // Simulate OCR processing (skeleton only - no real implementation)
        String result = "Result from OCR Worker - processed file: " + message.getFilename();

        // Send result back to result queue
        log.info("OCR Worker sending result to queue: {}", result);
        rabbitTemplate.convertAndSend(resultQueueName, result);
        log.info("OCR Worker result sent successfully");
    }
}

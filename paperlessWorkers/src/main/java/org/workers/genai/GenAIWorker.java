package org.workers.genai;

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
public class GenAIWorker {

    private final RabbitTemplate rabbitTemplate;
    private final FileStorage fileStorage;

    @Value("${rabbitmq.queue.genai.result}")
    private String resultQueueName;

    @RabbitListener(queues = "genai-worker-queue")
    public void processGenAiTask(FileMessageDto message) {
        log.info("GenAI Worker received message: {}", message);

        try {
            // Download file from MinIO
            byte[] fileContent = fileStorage.download(message.getObjectKey());
            log.info("GenAI Worker downloaded file from MinIO: {} ({} bytes)", 
                    message.getObjectKey(), fileContent.length);

            // Simulate GenAI processing (skeleton only - no real implementation) -> TODO
            String result = "Result from GenAI Worker - processed file: " + message.getFilename()
                    + " (" + fileContent.length + " bytes)";

            // Send result back to result queue
            log.info("GenAI Worker sending result to queue: {}", result);
            rabbitTemplate.convertAndSend(resultQueueName, result);
            log.info("GenAI Worker result sent successfully");
        } catch (Exception e) {
            log.error("GenAI Worker failed to process file: {}", e.getMessage(), e);
            // TODO: proper error handling
        }
    }
}
package org.workers.genai;

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
public class GenAIWorker {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.queue.genai.result}")
    private String resultQueueName;

    @RabbitListener(queues = "genai-worker-queue")
    public void processGenAiTask(FileMessageDto message) {
        log.info("GenAI Worker received message: {}", message);

        // Simulate GenAI processing (skeleton only - no real implementation)
        String result = "Result from GenAI Worker - processed file: " + message.getFilename();

        // Send result back to result queue
        log.info("GenAI Worker sending result to queue: {}", result);
        rabbitTemplate.convertAndSend(resultQueueName, result);
        log.info("GenAI Worker result sent successfully");
    }
}

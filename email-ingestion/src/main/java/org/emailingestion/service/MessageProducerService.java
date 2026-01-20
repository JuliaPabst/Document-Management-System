package org.emailingestion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.emailingestion.dto.FileMessageDto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Publishes file processing messages to RabbitMQ queues (OCR and GenAI)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageProducerService {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.queue.ocr}")
    private String ocrQueueName;

    @Value("${rabbitmq.queue.genai}")
    private String genaiQueueName;

    public void sendToOcrQueue(FileMessageDto message) {
        log.info("SENDING message to OCR queue: {}", message);
        rabbitTemplate.convertAndSend(ocrQueueName, message);
    }

    public void sendToGenAiQueue(FileMessageDto message) {
        log.info("SENDING message to GenAI queue: {}", message);
        rabbitTemplate.convertAndSend(genaiQueueName, message);
    }
}

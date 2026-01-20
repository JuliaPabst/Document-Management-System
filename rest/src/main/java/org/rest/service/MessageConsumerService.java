package org.rest.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for processing messages received from RabbitMQ queues (OCR and GenAI results)
 */
@Service
@Slf4j
public class MessageConsumerService {

    public void processOcrResult(String result) {
        log.info("RECEIVED OCR result: {}", result);
    }

    public void processGenAiResult(String result) {
        log.info("RECEIVED GenAI result: {}", result);
    }
}

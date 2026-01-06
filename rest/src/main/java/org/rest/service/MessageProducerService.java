package org.rest.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rest.dto.DocumentIndexDto;
import org.rest.dto.DocumentUpdateEventDto;
import org.rest.dto.FileMessageDto;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageProducerService {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.queue.ocr}")
    private String ocrQueueName;

    @Value("${rabbitmq.queue.genai}")
    private String genaiQueueName;
    
    @Value("${rabbitmq.queue.search-indexing}")
    private String searchIndexingQueue;

    public void sendToOcrQueue(FileMessageDto message) {
        log.info("SENDING message to OCR queue: {}", message);
        rabbitTemplate.convertAndSend(ocrQueueName, message);
    }

    public void sendToGenAiQueue(FileMessageDto message) {
        log.info("SENDING message to GenAI queue: {}", message);
        rabbitTemplate.convertAndSend(genaiQueueName, message);
    }
    
    public void sendDocumentUpdateEvent(DocumentUpdateEventDto event) {
        log.info("SENDING document {} event to search-indexing queue for document ID: {}", 
                event.getEventType(), event.getDocumentId());
        rabbitTemplate.convertAndSend(searchIndexingQueue, event);
    }
    
    public void sendDocumentForIndexing(DocumentIndexDto documentIndexDto) {
        log.info("SENDING document for indexing to search-indexing queue: document ID {}", 
                documentIndexDto.getDocumentId());
        rabbitTemplate.convertAndSend(searchIndexingQueue, documentIndexDto);
    }
}

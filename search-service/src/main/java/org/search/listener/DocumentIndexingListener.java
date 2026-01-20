package org.search.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.search.dto.DocumentIndexDto;
import org.search.dto.DocumentUpdateEventDto;
import org.search.service.ElasticsearchService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * RabbitMQ listener for document indexing events.
 * Processes new document indexing, updates and deletions in Elasticsearch.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentIndexingListener {

    private final ElasticsearchService elasticsearchService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${rabbitmq.queue.search-indexing}")
    public void handleDocumentIndexing(Message message) {
        log.info("Received message for search-indexing queue");

        try {
            // Parse message body to determine the type
            String body = new String(message.getBody());
            Map<String, Object> map = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
            
            if (map.containsKey("eventType")) {
                DocumentUpdateEventDto event = objectMapper.readValue(body, DocumentUpdateEventDto.class);
                log.info("Processing {} event for document ID: {}",
                        event.getEventType(), event.getDocumentId());
                
                if (event.getEventType() == DocumentUpdateEventDto.EventType.UPDATE) {
                    // Convert to DocumentIndexDto and perform partial update
                    // Only include fields that should be updated (don't include extractedText to preserve it)
                    DocumentIndexDto document = DocumentIndexDto.builder()
                            .documentId(event.getDocumentId())
                            .filename(event.getFilename())
                            .author(event.getAuthor())
                            .fileType(event.getFileType())
                            .size(event.getSize())
                            .objectKey(event.getObjectKey())
                            .summary(event.getSummary())
                            // extractedText will be preserved in Elasticsearch
                            .build();
                    elasticsearchService.updateDocumentPartial(document); // Partial update preserves extractedText
                    log.info("Successfully updated document in Elasticsearch: {}", event.getDocumentId());
                    
                } else if (event.getEventType() == DocumentUpdateEventDto.EventType.DELETE) {
                    elasticsearchService.deleteDocument(event.getDocumentId());
                    log.info("Successfully deleted document from Elasticsearch: {}", event.getDocumentId());
                }
            } else {
                DocumentIndexDto document = objectMapper.readValue(body, DocumentIndexDto.class);
                log.info("Processing NEW/INDEX event for document ID: {}, filename: {}",
                        document.getDocumentId(), document.getFilename());
                elasticsearchService.indexDocument(document);
                log.info("Successfully indexed document: {}", document.getDocumentId());
            }
        } catch (IOException e) {
            log.error("Failed to process search-indexing message: {}", e.getMessage(), e);
        }
    }
}

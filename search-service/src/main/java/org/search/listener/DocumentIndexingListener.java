package org.search.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.search.dto.DocumentIndexDto;
import org.search.service.ElasticsearchService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentIndexingListener {

    private final ElasticsearchService elasticsearchService;

    @RabbitListener(queues = "${rabbitmq.queue.search-indexing}")
    public void handleDocumentIndexing(DocumentIndexDto document) {
        log.info("Received document for indexing: ID={}, filename={}",
                document.getDocumentId(), document.getFilename());

        try {
            elasticsearchService.indexDocument(document);
            log.info("Successfully indexed document: {}", document.getDocumentId());
        } catch (IOException e) {
            log.error("Failed to index document {}: {}", document.getDocumentId(), e.getMessage(), e);
        }
    }
}

package org.batch.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.batch.dto.DocumentAccessRecord;
import org.batch.model.DocumentAccessStatistics;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * ItemProcessor to transform DocumentAccessRecord into DocumentAccessStatistics
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessLogProcessor implements ItemProcessor<DocumentAccessRecord, DocumentAccessStatistics> {

    private final AccessLogXmlReader reader;

    @Override
    public DocumentAccessStatistics process(DocumentAccessRecord record) throws Exception {
        // Validation
        if (record.getDocumentId() == null || record.getAccessCount() == null) {
            log.warn("Skipping invalid record: documentId={}, accessCount={}", 
                    record.getDocumentId(), record.getAccessCount());
            return null;
        }

        if (record.getAccessCount() < 0) {
            log.warn("Skipping record with negative access count: documentId={}, count={}", 
                    record.getDocumentId(), record.getAccessCount());
            return null;
        }

        // Transform to entity
        DocumentAccessStatistics stats = DocumentAccessStatistics.builder()
                .documentId(record.getDocumentId())
                .accessDate(LocalDate.now().minusDays(1)) // Previous day's statistics
                .accessCount(record.getAccessCount())
                .sourceFile(reader.getCurrentFileName())
                .build();

        log.debug("Processed access record: documentId={}, count={}", 
                record.getDocumentId(), record.getAccessCount());

        return stats;
    }
}

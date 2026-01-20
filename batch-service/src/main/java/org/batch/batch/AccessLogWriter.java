package org.batch.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.batch.model.DocumentAccessStatistics;
import org.batch.repository.DocumentAccessStatisticsRepository;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * ItemWriter to persist DocumentAccessStatistics to the database
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessLogWriter implements ItemWriter<DocumentAccessStatistics> {

    private final DocumentAccessStatisticsRepository repository;

    @Override
    public void write(Chunk<? extends DocumentAccessStatistics> chunk) throws Exception {
        List<? extends DocumentAccessStatistics> items = chunk.getItems();
        
        if (items.isEmpty()) {
            return;
        }

        log.info("Writing chunk of {} access statistics records", items.size());

        int updatedCount = 0;
        int createdCount = 0;
        int failedCount = 0;

        List<DocumentAccessStatistics> toSave = new ArrayList<>();

        for (DocumentAccessStatistics stats : items) {
            try {
                // Check if record already exists for this document and date
                var existing = repository.findByDocumentIdAndAccessDate(
                        stats.getDocumentId(), stats.getAccessDate());

                if (existing.isPresent()) {
                    // Update existing record (accumulate access counts)
                    DocumentAccessStatistics existingStats = existing.get();
                    existingStats.setAccessCount(existingStats.getAccessCount() + stats.getAccessCount());
                    existingStats.setSourceFile(stats.getSourceFile());
                    existingStats.setProcessedAt(Instant.now());
                    toSave.add(existingStats);
                    updatedCount++;
                    log.debug("Updating existing record: documentId={}, date={}, newTotal={}", 
                            stats.getDocumentId(), stats.getAccessDate(), existingStats.getAccessCount());
                } else {
                    // Create new record
                    toSave.add(stats);
                    createdCount++;
                }
            } catch (Exception e) {
                failedCount++;
                log.error("Failed to process access statistic for documentId={}: {}", 
                        stats.getDocumentId(), e.getMessage());
            }
        }

        // Batch save all records
        if (!toSave.isEmpty()) {
            repository.saveAll(toSave);
        }

        log.info("Chunk processing complete: {} created, {} updated, {} failed", 
                createdCount, updatedCount, failedCount);
    }
}

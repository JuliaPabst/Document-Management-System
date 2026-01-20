package org.batch.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentAccessStatisticsTest {

    @Test
    void testOnCreateSetsProcessedAtWhenNull() {
        // Arrange
        DocumentAccessStatistics stats = DocumentAccessStatistics.builder()
                .documentId(100L)
                .accessDate(LocalDate.now())
                .accessCount(10)
                .build();

        // Act
        stats.onCreate();

        // Assert
        assertThat(stats.getProcessedAt()).isNotNull();
    }
}

package org.batch.batch;

import org.batch.dto.DocumentAccessRecord;
import org.batch.model.DocumentAccessStatistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessLogProcessorTest {

    @Mock
    private AccessLogXmlReader reader;

    @InjectMocks
    private AccessLogProcessor processor;

    @Test
    void testProcessValidRecord() throws Exception {
        // Arrange
        when(reader.getCurrentFileName()).thenReturn("access-log-2026-01-18.xml");
        DocumentAccessRecord record = new DocumentAccessRecord(123L, 42, null);

        // Act
        DocumentAccessStatistics result = processor.process(record);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDocumentId()).isEqualTo(123L);
        assertThat(result.getAccessCount()).isEqualTo(42);
        assertThat(result.getAccessDate()).isEqualTo(LocalDate.now().minusDays(1));
        assertThat(result.getSourceFile()).isEqualTo("access-log-2026-01-18.xml");
    }

    @Test
    void testProcessRecordWithNullDocumentId() throws Exception {
        // Arrange
        DocumentAccessRecord record = new DocumentAccessRecord(null, 42, null);

        // Act
        DocumentAccessStatistics result = processor.process(record);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void testProcessRecordWithNullAccessCount() throws Exception {
        // Arrange
        DocumentAccessRecord record = new DocumentAccessRecord(123L, null, null);

        // Act
        DocumentAccessStatistics result = processor.process(record);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void testProcessRecordWithNegativeAccessCount() throws Exception {
        // Arrange
        DocumentAccessRecord record = new DocumentAccessRecord(123L, -5, null);

        // Act
        DocumentAccessStatistics result = processor.process(record);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void testProcessRecordWithZeroAccessCount() throws Exception {
        // Arrange
        DocumentAccessRecord record = new DocumentAccessRecord(123L, 0, null);

        // Act
        DocumentAccessStatistics result = processor.process(record);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAccessCount()).isEqualTo(0);
    }
}

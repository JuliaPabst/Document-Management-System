package org.batch.batch;

import org.batch.model.DocumentAccessStatistics;
import org.batch.repository.DocumentAccessStatisticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessLogWriterTest {

    @Mock
    private DocumentAccessStatisticsRepository repository;

    @InjectMocks
    private AccessLogWriter writer;

    private DocumentAccessStatistics testStats;

    @BeforeEach
    void setUp() {
        testStats = DocumentAccessStatistics.builder()
                .documentId(123L)
                .accessDate(LocalDate.of(2026, 1, 17))
                .accessCount(50)
                .sourceFile("test.xml")
                .processedAt(Instant.now())
                .build();
    }

    @Test
    void testWriteNewRecord() throws Exception {
        // Arrange
        Chunk<DocumentAccessStatistics> chunk = new Chunk<>(testStats);
        when(repository.findByDocumentIdAndAccessDate(123L, LocalDate.of(2026, 1, 17)))
                .thenReturn(Optional.empty());

        // Act
        writer.write(chunk);

        // Assert
        verify(repository).findByDocumentIdAndAccessDate(123L, LocalDate.of(2026, 1, 17));
        verify(repository).saveAll(argThat(iterable -> {
            List<DocumentAccessStatistics> list = StreamSupport.stream(iterable.spliterator(), false)
                    .collect(Collectors.toList());
            return list.size() == 1 && 
                   list.get(0).getDocumentId().equals(123L) &&
                   list.get(0).getAccessCount().equals(50);
        }));
    }

    @Test
    void testWriteUpdateExistingRecord() throws Exception {
        // Arrange
        DocumentAccessStatistics existing = DocumentAccessStatistics.builder()
                .id(1L)
                .documentId(123L)
                .accessDate(LocalDate.of(2026, 1, 17))
                .accessCount(30)
                .sourceFile("old.xml")
                .processedAt(Instant.now().minusSeconds(3600))
                .build();

        Chunk<DocumentAccessStatistics> chunk = new Chunk<>(testStats);
        when(repository.findByDocumentIdAndAccessDate(123L, LocalDate.of(2026, 1, 17)))
                .thenReturn(Optional.of(existing));

        // Act
        writer.write(chunk);

        // Assert
        verify(repository).findByDocumentIdAndAccessDate(123L, LocalDate.of(2026, 1, 17));
        verify(repository).saveAll(argThat(iterable -> {
            List<DocumentAccessStatistics> list = StreamSupport.stream(iterable.spliterator(), false)
                    .collect(Collectors.toList());
            return list.size() == 1 &&
                   list.get(0).getAccessCount() == 80 &&
                   list.get(0).getSourceFile().equals("test.xml");
        }));
    }

    @Test
    void testWriteEmptyChunk() throws Exception {
        // Arrange
        Chunk<DocumentAccessStatistics> emptyChunk = new Chunk<>();

        // Act
        writer.write(emptyChunk);

        // Assert
        verify(repository, never()).save(any());
        verify(repository, never()).findByDocumentIdAndAccessDate(any(), any());
    }

    @Test
    void testWriteMultipleRecords() throws Exception {
        // Arrange
        DocumentAccessStatistics stats1 = DocumentAccessStatistics.builder()
                .documentId(1L)
                .accessDate(LocalDate.of(2026, 1, 17))
                .accessCount(10)
                .sourceFile("test1.xml")
                .build();

        DocumentAccessStatistics stats2 = DocumentAccessStatistics.builder()
                .documentId(2L)
                .accessDate(LocalDate.of(2026, 1, 17))
                .accessCount(20)
                .sourceFile("test2.xml")
                .build();

        Chunk<DocumentAccessStatistics> chunk = new Chunk<>(stats1, stats2);
        when(repository.findByDocumentIdAndAccessDate(any(), any()))
                .thenReturn(Optional.empty());

        // Act
        writer.write(chunk);

        // Assert
        verify(repository, times(2)).findByDocumentIdAndAccessDate(any(), any());
        verify(repository).saveAll(argThat(iterable -> {
            List<DocumentAccessStatistics> list = StreamSupport.stream(iterable.spliterator(), false)
                    .collect(Collectors.toList());
            return list.size() == 2;
        }));
    }
}

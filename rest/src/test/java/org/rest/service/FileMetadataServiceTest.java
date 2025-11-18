
package org.rest.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rest.exception.FileMetadataNotFoundException;
import org.rest.model.FileMetadata;
import org.rest.repository.FileMetadataRepository;
import java.util.Optional;
import java.util.Collections;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class FileMetadataServiceTest {
    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Mock
    private MessageProducerService messageProducerService;

    @InjectMocks
    private FileMetadataService fileMetadataService;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    void testCreateFileMetadata() {
        FileMetadata inputEntity = new FileMetadata();
        inputEntity.setFilename("test.pdf");
        inputEntity.setAuthor("Tester");
        inputEntity.setFileType("pdf");
        inputEntity.setSize(123L);
        
        FileMetadata savedEntity = new FileMetadata();
        savedEntity.setId(1L);
        savedEntity.setFilename("test.pdf");
        savedEntity.setAuthor("Tester");
        savedEntity.setFileType("pdf");
        savedEntity.setSize(123L);
        
        when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(savedEntity);
        FileMetadata response = fileMetadataService.createFileMetadata(inputEntity);
        assertEquals("test.pdf", response.getFilename());
        assertEquals("Tester", response.getAuthor());
        assertEquals(1L, response.getId());
    }

    @Test
    void testGetFileMetadataByIdFound() {
        FileMetadata entity = new FileMetadata();
        entity.setId(2L);
        entity.setFilename("found.pdf");
        entity.setAuthor("Author");
        entity.setFileType("pdf");
        entity.setSize(456L);
        when(fileMetadataRepository.findById(2L)).thenReturn(Optional.of(entity));
        FileMetadata response = fileMetadataService.getFileMetadataById(2L);
        assertEquals("found.pdf", response.getFilename());
        assertEquals(2L, response.getId());
    }

    @Test
    void testGetFileMetadataByIdNotFound() {
        when(fileMetadataRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(FileMetadataNotFoundException.class, () -> fileMetadataService.getFileMetadataById(99L));
    }

    @Test
    void testUpdateFileMetadata() {
        FileMetadata entity = new FileMetadata();
        entity.setId(3L);
        entity.setFilename("old.pdf");
        entity.setAuthor("Old Author");
        entity.setFileType("pdf");
        entity.setSize(789L);
        
        FileMetadata updateEntity = new FileMetadata();
        updateEntity.setFilename("new.pdf");
        updateEntity.setAuthor("New Author");
        updateEntity.setFileType("docx");
        updateEntity.setSize(101L);
        
        when(fileMetadataRepository.findById(3L)).thenReturn(Optional.of(entity));
        when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(entity);
        FileMetadata response = fileMetadataService.updateFileMetadata(3L, updateEntity);
        assertEquals("new.pdf", response.getFilename());
        assertEquals("New Author", response.getAuthor());
        assertEquals("docx", response.getFileType());
        assertEquals(101L, response.getSize());
    }

    @Test
    void testUpdateFileMetadataNotFound() {
        FileMetadata updateEntity = new FileMetadata();
        when(fileMetadataRepository.findById(42L)).thenReturn(Optional.empty());
        assertThrows(FileMetadataNotFoundException.class, () -> fileMetadataService.updateFileMetadata(42L, updateEntity));
    }

    @Test
    void testDeleteFileMetadata() {
        when(fileMetadataRepository.existsById(4L)).thenReturn(true);
        doNothing().when(fileMetadataRepository).deleteById(4L);
        fileMetadataService.deleteFileMetadata(4L);
        verify(fileMetadataRepository, times(1)).deleteById(4L);
    }

    @Test
    void testDeleteFileMetadataNotFound() {
        when(fileMetadataRepository.existsById(5L)).thenReturn(false);
        assertThrows(FileMetadataNotFoundException.class, () -> fileMetadataService.deleteFileMetadata(5L));
    }

    @Test
    void testGetAllFileMetadata() {
        FileMetadata entity = new FileMetadata();
        entity.setId(6L);
        entity.setFilename("all.pdf");
        entity.setAuthor("All Author");
        entity.setFileType("pdf");
        entity.setSize(222L);
        when(fileMetadataRepository.findByOrderByUploadTimeDesc()).thenReturn(List.of(entity));
        List<FileMetadata> result = fileMetadataService.getAllFileMetadata();
        assertEquals(1, result.size());
        assertEquals("all.pdf", result.getFirst().getFilename());
    }

    @Test
    void testGetAllFileMetadataEmpty() {
        when(fileMetadataRepository.findByOrderByUploadTimeDesc()).thenReturn(Collections.emptyList());
        List<FileMetadata> result = fileMetadataService.getAllFileMetadata();
        assertTrue(result.isEmpty());
    }

    @Test
    void testSearchFileMetadata() {
        FileMetadata entity = new FileMetadata();
        entity.setId(7L);
        entity.setFilename("search.pdf");
        entity.setAuthor("Search Author");
        entity.setFileType("pdf");
        entity.setSize(333L);
        when(fileMetadataRepository.searchByKeyword("search")).thenReturn(List.of(entity));
        List<FileMetadata> result = fileMetadataService.searchFileMetadata("search");
        assertEquals(1, result.size());
        assertEquals("search.pdf", result.getFirst().getFilename());
    }

    @Test
    void testGetFileMetadataByAuthor() {
        FileMetadata entity = new FileMetadata();
        entity.setId(8L);
        entity.setFilename("author.pdf");
        entity.setAuthor("Alice");
        entity.setFileType("pdf");
        entity.setSize(444L);
        when(fileMetadataRepository.findByAuthor("Alice")).thenReturn(List.of(entity));
        List<FileMetadata> result = fileMetadataService.getFileMetadataByAuthor("Alice");
        assertEquals(1, result.size());
        assertEquals("Alice", result.getFirst().getAuthor());
    }

    @Test
    void testGetFileMetadataByFileType() {
        FileMetadata entity = new FileMetadata();
        entity.setId(9L);
        entity.setFilename("type.pdf");
        entity.setAuthor("Type Author");
        entity.setFileType("pdf");
        entity.setSize(555L);
        when(fileMetadataRepository.findByFileType("pdf")).thenReturn(List.of(entity));
        List<FileMetadata> result = fileMetadataService.getFileMetadataByFileType("pdf");
        assertEquals(1, result.size());
        assertEquals("pdf", result.getFirst().getFileType());
    }

    @Test
    void testCreateFileMetadataWithWorkerNotification() {
        // arrange
        FileMetadata inputEntity = new FileMetadata();
        inputEntity.setFilename("test.pdf");
        inputEntity.setAuthor("Test Author");
        inputEntity.setFileType("PDF");
        inputEntity.setSize(1024L);

        FileMetadata savedEntity = new FileMetadata();
        savedEntity.setId(1L);
        savedEntity.setFilename("test.pdf");
        savedEntity.setAuthor("Test Author");
        savedEntity.setFileType("PDF");
        savedEntity.setSize(1024L);

        when(fileMetadataRepository.existsByFilenameAndAuthor(anyString(), anyString())).thenReturn(false);
        when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(savedEntity);

        // act
        FileMetadata result = fileMetadataService.createFileMetadataWithWorkerNotification(inputEntity);

        // assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(fileMetadataRepository).save(any(FileMetadata.class));
        verify(messageProducerService).sendToOcrQueue(any());
        verify(messageProducerService).sendToGenAiQueue(any());
    }

    @Test
    void testUpdateFileMetadataWithWorkerNotification_WithFileReplacement() {
        // arrange
        FileMetadata existing = new FileMetadata();
        existing.setId(1L);
        existing.setFilename("old.pdf");
        existing.setAuthor("Author");
        existing.setFileType("PDF");
        existing.setSize(1024L);

        FileMetadata updates = new FileMetadata();
        updates.setFilename("new.pdf");
        updates.setSize(2048L);

        when(fileMetadataRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(existing);

        // act
        FileMetadata result = fileMetadataService.updateFileMetadataWithWorkerNotification(1L, updates, true);

        // assert
        assertNotNull(result);
        verify(fileMetadataRepository).save(any(FileMetadata.class));
        verify(messageProducerService).sendToOcrQueue(any());
        verify(messageProducerService).sendToGenAiQueue(any());
    }

    @Test
    void testUpdateFileMetadataWithWorkerNotification_WithoutFileReplacement() {
        // arrange
        FileMetadata existing = new FileMetadata();
        existing.setId(1L);
        existing.setFilename("test.pdf");
        existing.setAuthor("Old Author");
        existing.setFileType("PDF");
        existing.setSize(1024L);

        FileMetadata updates = new FileMetadata();
        updates.setAuthor("New Author");

        when(fileMetadataRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(existing);

        // act
        FileMetadata result = fileMetadataService.updateFileMetadataWithWorkerNotification(1L, updates, false);

        // assert
        assertNotNull(result);
        verify(fileMetadataRepository).save(any(FileMetadata.class));
        verify(messageProducerService, never()).sendToOcrQueue(any());
        verify(messageProducerService, never()).sendToGenAiQueue(any());
    }
}
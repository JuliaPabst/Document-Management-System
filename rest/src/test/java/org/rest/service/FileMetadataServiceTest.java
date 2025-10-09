
package org.rest.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rest.dto.FileMetadataCreateDto;
import org.rest.dto.FileMetadataResponseDto;
import org.rest.dto.FileMetadataUpdateDto;
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

    @InjectMocks
    private FileMetadataService fileMetadataService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCreateFileMetadata() {
        FileMetadataCreateDto dto = new FileMetadataCreateDto();
        dto.setFilename("test.pdf");
        dto.setAuthor("Tester");
        dto.setFileType("pdf");
        dto.setSize(123L);
        FileMetadata entity = new FileMetadata();
        entity.setId(1L);
        entity.setFilename("test.pdf");
        entity.setAuthor("Tester");
        entity.setFileType("pdf");
        entity.setSize(123L);
        when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(entity);
        FileMetadataResponseDto response = fileMetadataService.createFileMetadata(dto);
        assertEquals("test.pdf", response.getFilename());
        assertEquals("Tester", response.getAuthor());
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
        FileMetadataResponseDto response = fileMetadataService.getFileMetadataById(2L);
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
        FileMetadataUpdateDto updateDto = new FileMetadataUpdateDto();
        updateDto.setFilename("new.pdf");
        updateDto.setAuthor("New Author");
        updateDto.setFileType("docx");
        updateDto.setSize(101L);
        when(fileMetadataRepository.findById(3L)).thenReturn(Optional.of(entity));
        when(fileMetadataRepository.save(any(FileMetadata.class))).thenReturn(entity);
        FileMetadataResponseDto response = fileMetadataService.updateFileMetadata(3L, updateDto);
        assertEquals("new.pdf", response.getFilename());
        assertEquals("New Author", response.getAuthor());
        assertEquals("docx", response.getFileType());
        assertEquals(101L, response.getSize());
    }

    @Test
    void testUpdateFileMetadataNotFound() {
        FileMetadataUpdateDto updateDto = new FileMetadataUpdateDto();
        when(fileMetadataRepository.findById(42L)).thenReturn(Optional.empty());
        assertThrows(FileMetadataNotFoundException.class, () -> fileMetadataService.updateFileMetadata(42L, updateDto));
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
        List<FileMetadataResponseDto> result = fileMetadataService.getAllFileMetadata();
        assertEquals(1, result.size());
        assertEquals("all.pdf", result.get(0).getFilename());
    }

    @Test
    void testGetAllFileMetadataEmpty() {
        when(fileMetadataRepository.findByOrderByUploadTimeDesc()).thenReturn(Collections.emptyList());
        List<FileMetadataResponseDto> result = fileMetadataService.getAllFileMetadata();
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
        List<FileMetadataResponseDto> result = fileMetadataService.searchFileMetadata("search");
        assertEquals(1, result.size());
        assertEquals("search.pdf", result.get(0).getFilename());
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
        List<FileMetadataResponseDto> result = fileMetadataService.getFileMetadataByAuthor("Alice");
        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).getAuthor());
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
        List<FileMetadataResponseDto> result = fileMetadataService.getFileMetadataByFileType("pdf");
        assertEquals(1, result.size());
        assertEquals("pdf", result.get(0).getFileType());
    }
}

package org.rest.controller;

import org.junit.jupiter.api.Test;
import org.rest.exception.FileMetadataNotFoundException;
import org.rest.exception.GlobalExceptionHandler;
import org.rest.mapper.FileMetadataMapper;
import org.rest.model.FileMetadata;
import org.rest.service.FileMetadataService;
import org.rest.service.FileStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FileMetadataController.class)
@Import(GlobalExceptionHandler.class)
class FileMetadataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Provide Service and Mapper with @MockitoBean
    @MockitoBean
    private FileMetadataService fileMetadataService;

    @MockitoBean
    private FileMetadataMapper fileMetadataMapper;

    @MockitoBean
    private FileStorage fileStorage;

    @Test
    void testUploadFile_Multipart_Returns201() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "new.pdf", "application/pdf", "dummy".getBytes()
        );
        Instant now = Instant.now();

        FileMetadata mapped = new FileMetadata();
        mapped.setFilename("new.pdf");
        mapped.setAuthor("Jane");
        mapped.setFileType("PDF");
        mapped.setSize((long) "dummy".getBytes().length);
        mapped.setUploadTime(now);
        mapped.setLastEdited(now);

        FileMetadata saved = new FileMetadata();
        saved.setId(42L);
        saved.setFilename(mapped.getFilename());
        saved.setAuthor(mapped.getAuthor());
        saved.setFileType(mapped.getFileType());
        saved.setSize(mapped.getSize());
        saved.setUploadTime(now);
        saved.setLastEdited(now);

        // Mapper/Service/Storage stubs
        when(fileMetadataMapper.toEntity(any(), any())).thenReturn(mapped);
        doNothing().when(fileStorage).upload(anyString(), any(byte[].class), anyString());
        when(fileMetadataService.createFileMetadataWithWorkerNotification(any(FileMetadata.class))).thenReturn(saved);
        when(fileMetadataMapper.toResponseDto(any(FileMetadata.class))).thenAnswer(inv -> {
            FileMetadata fm = inv.getArgument(0);
            var dto = new org.rest.dto.FileMetadataResponseDto();
            dto.setId(fm.getId());
            dto.setFilename(fm.getFilename());
            dto.setAuthor(fm.getAuthor());
            dto.setFileType(fm.getFileType());
            dto.setSize(fm.getSize());
            dto.setUploadTime(fm.getUploadTime());
            dto.setLastEdited(fm.getLastEdited());
            return dto;
        });

        // when/then
        mockMvc.perform(
                        multipart("/api/v1/files")
                                .file(file)
                                .param("author", "Jane")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.filename").value("new.pdf"))
                .andExpect(jsonPath("$.author").value("Jane"))
                .andExpect(jsonPath("$.fileType").value("PDF"));
    }

    @Test
    void testUploadFile_EmptyFile_Returns400() throws Exception {
        // given: empty upload -> Controller throws IllegalArgumentException
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "application/pdf", new byte[0]);

        // when/then
        mockMvc.perform(
                        multipart("/api/v1/files")
                                .file(emptyFile)
                                .param("author", "Jane")
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void testGetFileMetadataById() throws Exception {
        Instant now = Instant.now();
        FileMetadata entity = new FileMetadata();
        entity.setId(1L);
        entity.setFilename("test.pdf");
        entity.setAuthor("Tester");
        entity.setFileType("PDF");
        entity.setSize(123L);
        entity.setUploadTime(now);
        entity.setLastEdited(now);

        when(fileMetadataService.getFileMetadataById(1L)).thenReturn(entity);
        when(fileMetadataMapper.toResponseDto(any(FileMetadata.class))).thenAnswer(inv -> {
            FileMetadata fm = inv.getArgument(0);
            var dto = new org.rest.dto.FileMetadataResponseDto();
            dto.setId(fm.getId());
            dto.setFilename(fm.getFilename());
            dto.setAuthor(fm.getAuthor());
            dto.setFileType(fm.getFileType());
            dto.setSize(fm.getSize());
            dto.setUploadTime(fm.getUploadTime());
            dto.setLastEdited(fm.getLastEdited());
            return dto;
        });

        mockMvc.perform(get("/api/v1/files/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.filename").value("test.pdf"));
    }

    @Test
    void testGetFileMetadataById_NotFound_Returns404() throws Exception {
        when(fileMetadataService.getFileMetadataById(999L))
                .thenThrow(new FileMetadataNotFoundException("File metadata not found with ID: 999"));

        mockMvc.perform(get("/api/v1/files/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("File metadata not found with ID: 999"))
                .andExpect(jsonPath("$.path").value("/api/v1/files/999"));
    }

    @Test
    void testUpdateFileMetadata_WithFileAndAuthor() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "updated.pdf", "application/pdf", "updated content".getBytes()
        );
        Instant now = Instant.now();

        FileMetadata existing = new FileMetadata();
        existing.setId(1L);
        existing.setObjectKey("old-objectKey");

        FileMetadata updated = new FileMetadata();
        updated.setId(1L);
        updated.setFilename("updated.pdf");
        updated.setAuthor("Updated Author");
        updated.setFileType("PDF");
        updated.setSize((long) "updated content".getBytes().length);
        updated.setUploadTime(now);
        updated.setLastEdited(now);

        when(fileMetadataService.getFileMetadataById(1L)).thenReturn(existing);
        when(fileMetadataMapper.extractExtensionUpper("updated.pdf")).thenReturn("PDF");
        doNothing().when(fileStorage).upload(anyString(), any(byte[].class), anyString());
        doNothing().when(fileStorage).delete(anyString());
        when(fileMetadataService.updateFileMetadataWithWorkerNotification(eq(1L), any(FileMetadata.class), eq(true))).thenReturn(updated);
        when(fileMetadataMapper.toResponseDto(any(FileMetadata.class))).thenAnswer(inv -> {
            FileMetadata fm = inv.getArgument(0);
            var dto = new org.rest.dto.FileMetadataResponseDto();
            dto.setId(fm.getId());
            dto.setFilename(fm.getFilename());
            dto.setAuthor(fm.getAuthor());
            dto.setFileType(fm.getFileType());
            dto.setSize(fm.getSize());
            dto.setUploadTime(fm.getUploadTime());
            dto.setLastEdited(fm.getLastEdited());
            return dto;
        });

        mockMvc.perform(
                        multipart("/api/v1/files/1")
                                .file(file)
                                .param("author", "Updated Author")
                                .with(request -> {
                                    request.setMethod("PATCH");
                                    return request;
                                })
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("updated.pdf"))
                .andExpect(jsonPath("$.author").value("Updated Author"))
                .andExpect(jsonPath("$.fileType").value("PDF"));
    }

    @Test
    void testUpdateFileMetadata_OnlyAuthor() throws Exception {
        // given - update only author, no file
        Instant now = Instant.now();

        FileMetadata updated = new FileMetadata();
        updated.setId(1L);
        updated.setFilename("original.pdf");
        updated.setAuthor("New Author");
        updated.setFileType("PDF");
        updated.setSize(1024L);
        updated.setUploadTime(now);
        updated.setLastEdited(now);

        when(fileMetadataService.updateFileMetadataWithWorkerNotification(eq(1L), any(FileMetadata.class), eq(false))).thenReturn(updated);
        when(fileMetadataMapper.toResponseDto(any(FileMetadata.class))).thenAnswer(inv -> {
            FileMetadata fm = inv.getArgument(0);
            var dto = new org.rest.dto.FileMetadataResponseDto();
            dto.setId(fm.getId());
            dto.setFilename(fm.getFilename());
            dto.setAuthor(fm.getAuthor());
            dto.setFileType(fm.getFileType());
            dto.setSize(fm.getSize());
            dto.setUploadTime(fm.getUploadTime());
            dto.setLastEdited(fm.getLastEdited());
            return dto;
        });

        mockMvc.perform(
                        multipart("/api/v1/files/1")
                                .param("author", "New Author")
                                .with(request -> {
                                    request.setMethod("PATCH");
                                    return request;
                                })
                                .contentType(MediaType.MULTIPART_FORM_DATA)
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.author").value("New Author"));
    }

    @Test
    void testDeleteFileMetadata() throws Exception {
        FileMetadata existing = new FileMetadata();
        existing.setId(1L);
        existing.setObjectKey("test-objectKey");

        when(fileMetadataService.getFileMetadataById(1L)).thenReturn(existing);
        doNothing().when(fileMetadataService).deleteFileMetadata(1L);
        doNothing().when(fileStorage).delete(anyString());

        mockMvc.perform(delete("/api/v1/files/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void testGetAllFileMetadata() throws Exception {
        Instant now = Instant.now();

        FileMetadata entity = new FileMetadata();
        entity.setId(1L);
        entity.setFilename("test.pdf");
        entity.setAuthor("Tester");
        entity.setFileType("PDF");
        entity.setSize(123L);
        entity.setUploadTime(now);
        entity.setLastEdited(now);

        when(fileMetadataService.getAllFileMetadata()).thenReturn(List.of(entity));
        when(fileMetadataMapper.toResponseDtoList(any())).thenAnswer(inv -> {
            List<FileMetadata> list = inv.getArgument(0);
            return list.stream().map(fm -> {
                var dto = new org.rest.dto.FileMetadataResponseDto();
                dto.setId(fm.getId());
                dto.setFilename(fm.getFilename());
                dto.setAuthor(fm.getAuthor());
                dto.setFileType(fm.getFileType());
                dto.setSize(fm.getSize());
                dto.setUploadTime(fm.getUploadTime());
                dto.setLastEdited(fm.getLastEdited());
                return dto;
            }).toList();
        });

        mockMvc.perform(get("/api/v1/files"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].filename").value("test.pdf"));
    }
}
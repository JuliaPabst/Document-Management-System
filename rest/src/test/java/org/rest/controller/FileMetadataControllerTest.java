package org.rest.controller;

import org.junit.jupiter.api.Test;
import org.rest.dto.FileMetadataCreateDto;
import org.rest.dto.FileMetadataUpdateDto;
import org.rest.dto.FileMetadataResponseDto;
import org.rest.exception.FileMetadataNotFoundException;
import org.rest.exception.GlobalExceptionHandler;
import org.rest.service.FileMetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FileMetadataController.class)
@Import(GlobalExceptionHandler.class)
class FileMetadataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FileMetadataService fileMetadataService;

    @Test
    void testGetFileMetadataById() throws Exception {
        FileMetadataResponseDto responseDto =
                new FileMetadataResponseDto(1L, "test.pdf", "Tester", "pdf", 123L, null, null);

        when(fileMetadataService.getFileMetadataById(1L)).thenReturn(responseDto);

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
    void testCreateFileMetadata() throws Exception {
        FileMetadataResponseDto responseDto =
                new FileMetadataResponseDto(1L, "test.pdf", "Tester", "pdf", 123L, null, null);

        when(fileMetadataService.createFileMetadata(any(FileMetadataCreateDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filename\":\"test.pdf\",\"author\":\"Tester\",\"fileType\":\"pdf\",\"size\":123}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Content-Type"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void testCreateFileMetadata_ValidationError_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"author\":\"Tester\",\"fileType\":\"pdf\",\"size\":123}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void testUpdateFileMetadata() throws Exception {
        FileMetadataResponseDto responseDto =
                new FileMetadataResponseDto(1L, "updated.pdf", "Tester", "pdf", 123L, null, null);

        when(fileMetadataService.updateFileMetadata(eq(1L), any(FileMetadataUpdateDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(patch("/api/v1/files/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"filename\":\"updated.pdf\",\"author\":\"Tester\",\"fileType\":\"pdf\",\"size\":123}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("updated.pdf"));
    }

    @Test
    void testDeleteFileMetadata() throws Exception {
        doNothing().when(fileMetadataService).deleteFileMetadata(1L);

        mockMvc.perform(delete("/api/v1/files/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void testGetAllFileMetadata() throws Exception {
        FileMetadataResponseDto responseDto =
                new FileMetadataResponseDto(1L, "test.pdf", "Tester", "pdf", 123L, null, null);

        when(fileMetadataService.getAllFileMetadata()).thenReturn(java.util.List.of(responseDto));

        mockMvc.perform(get("/api/v1/files"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].filename").value("test.pdf"));
    }
}
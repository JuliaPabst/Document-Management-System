
package org.rest.controller;

import org.junit.jupiter.api.Test;
import org.rest.dto.FileMetadataCreateDto;
import org.rest.dto.FileMetadataUpdateDto;
import org.rest.dto.FileMetadataResponseDto;
import org.rest.service.FileMetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import static org.mockito.Mockito.*;

@SpringBootTest
public class FileMetadataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileMetadataService fileMetadataService;

    @Test
    void testGetFileMetadataById() throws Exception {
        FileMetadataResponseDto responseDto = new FileMetadataResponseDto(1L, "test.pdf", "Tester", "pdf", 123L, null, null);
        when(fileMetadataService.getFileMetadataById(1L)).thenReturn(responseDto);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/files/1"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void testCreateFileMetadata() throws Exception {
        FileMetadataResponseDto responseDto = new FileMetadataResponseDto(1L, "test.pdf", "Tester", "pdf", 123L, null, null);
        when(fileMetadataService.createFileMetadata(any(FileMetadataCreateDto.class))).thenReturn(responseDto);
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/files")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"filename\":\"test.pdf\",\"author\":\"Tester\",\"fileType\":\"pdf\",\"size\":123}"))
                .andExpect(MockMvcResultMatchers.status().isCreated());
    }

    @Test
    void testUpdateFileMetadata() throws Exception {
        FileMetadataResponseDto responseDto = new FileMetadataResponseDto(1L, "updated.pdf", "Tester", "pdf", 123L, null, null);
        when(fileMetadataService.updateFileMetadata(eq(1L), any(FileMetadataUpdateDto.class))).thenReturn(responseDto);
        mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/files/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"filename\":\"updated.pdf\",\"author\":\"Tester\",\"fileType\":\"pdf\",\"size\":123}"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void testDeleteFileMetadata() throws Exception {
        doNothing().when(fileMetadataService).deleteFileMetadata(1L);
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/files/1"))
                .andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    @Test
    void testGetAllFileMetadata() throws Exception {
        FileMetadataResponseDto responseDto = new FileMetadataResponseDto(1L, "test.pdf", "Tester", "pdf", 123L, null, null);
        when(fileMetadataService.getAllFileMetadata()).thenReturn(java.util.List.of(responseDto));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/files"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }
}

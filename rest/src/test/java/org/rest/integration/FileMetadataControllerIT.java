package org.rest.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.rest.config.TestcontainersConfiguration;
import org.rest.model.FileMetadata;
import org.rest.repository.FileMetadataRepository;
import org.rest.service.FileStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for FileMetadataController
 * Uses MockMvc, Testcontainers for DB, and mocks MinIO storage
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.rabbitmq.host=localhost",
        "spring.rabbitmq.port=5672"
})
@DisplayName("FileMetadata Controller Integration Tests")
class FileMetadataControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @MockitoBean
    private FileStorage fileStorage;

    @BeforeEach
    void setUp() {
        fileMetadataRepository.deleteAll();
        reset(fileStorage);
    }

    @Test
    @DisplayName("Should upload file and create metadata successfully")
    @Transactional
    void uploadFile() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-document.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "PDF content".getBytes()
        );

        doNothing().when(fileStorage).upload(anyString(), any(byte[].class), anyString());

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/files")
                        .file(file)
                        .param("author", "John Doe"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.filename").value("test-document.pdf"))
                .andExpect(jsonPath("$.author").value("John Doe"))
                .andExpect(jsonPath("$.fileType").value(MediaType.APPLICATION_PDF_VALUE))
                .andExpect(jsonPath("$.size").value(11))
                .andExpect(jsonPath("$.objectKey").isNotEmpty())
                .andExpect(jsonPath("$.uploadTime").isNotEmpty());

        // Verify storage was called
        verify(fileStorage, times(1)).upload(anyString(), any(byte[].class), eq(MediaType.APPLICATION_PDF_VALUE));

        // Verify database
        List<FileMetadata> files = fileMetadataRepository.findAll();
        assertThat(files).hasSize(1);
        assertThat(files.get(0).getFilename()).isEqualTo("test-document.pdf");
        assertThat(files.get(0).getAuthor()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should return 400 when uploading empty file")
    void uploadEmptyFile() throws Exception {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                new byte[0]
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/files")
                        .file(emptyFile)
                        .param("author", "John Doe"))
                .andExpect(status().isBadRequest());

        verify(fileStorage, never()).upload(anyString(), any(byte[].class), anyString());
    }

    @Test
    @DisplayName("Should retrieve file metadata by ID")
    @Transactional
    void getFileMetadataById() throws Exception {
        // Arrange
        FileMetadata metadata = new FileMetadata();
        metadata.setFilename("report.pdf");
        metadata.setAuthor("Jane Smith");
        metadata.setFileType("application/pdf");
        metadata.setSize(2048L);
        metadata.setObjectKey("obj-key-123");
        metadata.setUploadTime(Instant.now());
        metadata.setLastEdited(Instant.now());
        FileMetadata saved = fileMetadataRepository.save(metadata);

        // Act & Assert
        mockMvc.perform(get("/api/v1/files/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.filename").value("report.pdf"))
                .andExpect(jsonPath("$.author").value("Jane Smith"))
                .andExpect(jsonPath("$.fileType").value("application/pdf"))
                .andExpect(jsonPath("$.size").value(2048));
    }

    @Test
    @DisplayName("Should return 404 when file metadata not found")
    void getFileMetadataByIdNotFound() throws Exception {
        // Arrange
        // No file created
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/files/{id}", 99999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should search files by author")
    @Transactional
    void searchFilesByAuthor() throws Exception {
        // Arrange
        FileMetadata file1 = createFileMetadata("doc1.pdf", "John Doe", "application/pdf");
        FileMetadata file2 = createFileMetadata("doc2.pdf", "John Smith", "application/pdf");
        FileMetadata file3 = createFileMetadata("doc3.pdf", "Jane Doe", "application/pdf");

        fileMetadataRepository.saveAll(List.of(file1, file2, file3));

        // Act & Assert - Search for "John"
        mockMvc.perform(get("/api/v1/files/search")
                        .param("author", "John"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].author", hasItem(containsString("John"))));
    }

    @Test
    @DisplayName("Should search files by file type")
    @Transactional
    void searchFilesByFileType() throws Exception {
        // Arrange
        FileMetadata file1 = createFileMetadata("doc1.pdf", "John Doe", "application/pdf");
        FileMetadata file2 = createFileMetadata("doc2.docx", "Jane Smith", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        FileMetadata file3 = createFileMetadata("doc3.pdf", "Bob Johnson", "application/pdf");

        fileMetadataRepository.saveAll(List.of(file1, file2, file3));

        // Act & Assert - Search for PDF files
        mockMvc.perform(get("/api/v1/files/search")
                        .param("fileType", "pdf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].fileType", everyItem(containsString("pdf"))));
    }

    @Test
    @DisplayName("Should search files by keyword")
    @Transactional
    void searchFilesByKeyword() throws Exception {
        // Arrange
        FileMetadata file1 = createFileMetadata("important-report.pdf", "John Doe", "application/pdf");
        FileMetadata file2 = createFileMetadata("meeting-notes.docx", "Jane Smith", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        FileMetadata file3 = createFileMetadata("report-summary.pdf", "Bob Johnson", "application/pdf");

        fileMetadataRepository.saveAll(List.of(file1, file2, file3));

        // Act & Assert - Search for "report"
        mockMvc.perform(get("/api/v1/files/search")
                        .param("keyword", "report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].filename", hasItem(containsString("report"))));
    }

    @Test
    @DisplayName("Should get all files ordered by upload time descending")
    @Transactional
    void getAllFilesOrderedByUploadTime() throws Exception {
        // Arrange
        FileMetadata file1 = createFileMetadata("old-file.pdf", "John Doe", "application/pdf");
        file1.setUploadTime(Instant.now().minusSeconds(3600));
        
        FileMetadata file2 = createFileMetadata("recent-file.pdf", "Jane Smith", "application/pdf");
        file2.setUploadTime(Instant.now().minusSeconds(60));
        
        FileMetadata file3 = createFileMetadata("newest-file.pdf", "Bob Johnson", "application/pdf");
        file3.setUploadTime(Instant.now());

        fileMetadataRepository.saveAll(List.of(file1, file2, file3));

        // Act & Assert
        mockMvc.perform(get("/api/v1/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].filename").value("newest-file.pdf"))
                .andExpect(jsonPath("$[1].filename").value("recent-file.pdf"))
                .andExpect(jsonPath("$[2].filename").value("old-file.pdf"));
    }

    @Test
    @DisplayName("Should update file summary")
    @Transactional
    void updateFileSummary() throws Exception {
        // Arrange
        FileMetadata metadata = createFileMetadata("document.pdf", "John Doe", "application/pdf");
        FileMetadata saved = fileMetadataRepository.save(metadata);

        String updateRequest = "{\"summary\":\"This is an updated summary of the document\"}";

        // Act & Assert
        mockMvc.perform(patch("/api/v1/files/{id}/summary", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("This is an updated summary of the document"));

        // Verify in database
        Optional<FileMetadata> updated = fileMetadataRepository.findById(saved.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getSummary()).isEqualTo("This is an updated summary of the document");
    }

    @Test
    @DisplayName("Should delete file metadata")
    @Transactional
    void deleteFileMetadata() throws Exception {
        // Arrange
        FileMetadata metadata = createFileMetadata("to-delete.pdf", "John Doe", "application/pdf");
        FileMetadata saved = fileMetadataRepository.save(metadata);

        doNothing().when(fileStorage).delete(anyString());

        // Act & Assert
        mockMvc.perform(delete("/api/v1/files/{id}", saved.getId()))
                .andExpect(status().isNoContent());

        // Verify deletion
        Optional<FileMetadata> deleted = fileMetadataRepository.findById(saved.getId());
        assertThat(deleted).isEmpty();

        verify(fileStorage, times(1)).delete(saved.getObjectKey());
    }

    @Test
    @DisplayName("Should prevent duplicate file upload for same author")
    @Transactional
    void preventDuplicateFileUpload() throws Exception {
        // Arrange - Create existing file
        FileMetadata existing = createFileMetadata("duplicate.pdf", "John Doe", "application/pdf");
        fileMetadataRepository.save(existing);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "duplicate.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "PDF content".getBytes()
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/files")
                        .file(file)
                        .param("author", "John Doe"))
                .andExpect(status().isConflict());

        // Verify only one file exists
        List<FileMetadata> files = fileMetadataRepository.findByFilenameAndAuthor("duplicate.pdf", "John Doe");
        assertThat(files).hasSize(1);
    }

    @Test
    @DisplayName("Should handle file upload with special characters in filename")
    @Transactional
    void uploadFileWithSpecialCharacters() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-file-2024_v1.0 (final).pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "PDF content".getBytes()
        );

        doNothing().when(fileStorage).upload(anyString(), any(byte[].class), anyString());

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/files")
                        .file(file)
                        .param("author", "John Doe"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.filename").value("test-file-2024_v1.0 (final).pdf"));
    }

    // Helper method to create test file metadata
    private FileMetadata createFileMetadata(String filename, String author, String fileType) {
        FileMetadata metadata = new FileMetadata();
        metadata.setFilename(filename);
        metadata.setAuthor(author);
        metadata.setFileType(fileType);
        metadata.setSize(1024L);
        metadata.setObjectKey("obj-" + System.nanoTime());
        metadata.setUploadTime(Instant.now());
        metadata.setLastEdited(Instant.now());
        return metadata;
    }
}

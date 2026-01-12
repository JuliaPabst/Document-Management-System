package org.rest.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.rest.config.TestcontainersConfiguration;
import org.rest.model.FileMetadata;
import org.rest.repository.FileMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Integration tests for FileMetadataRepository using Testcontainers
 * Tests database operations with real PostgreSQL instance
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("FileMetadata Repository Integration Tests")
class FileMetadataRepositoryIT {

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    private FileMetadata testFile1;
    private FileMetadata testFile2;

    @BeforeEach
    void setUp() {
        fileMetadataRepository.deleteAll();

        testFile1 = new FileMetadata();
        testFile1.setFilename("test-document.pdf");
        testFile1.setAuthor("John Doe");
        testFile1.setFileType("application/pdf");
        testFile1.setSize(1024L);
        testFile1.setObjectKey("obj-key-1");
        testFile1.setUploadTime(Instant.now());
        testFile1.setLastEdited(Instant.now());

        testFile2 = new FileMetadata();
        testFile2.setFilename("report.docx");
        testFile2.setAuthor("Jane Smith");
        testFile2.setFileType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        testFile2.setSize(2048L);
        testFile2.setObjectKey("obj-key-2");
        testFile2.setUploadTime(Instant.now());
        testFile2.setLastEdited(Instant.now());
    }

    @Test
    @DisplayName("Should save and retrieve file metadata by ID")
    void saveAndFindById() {
        // Arrange
        // testFile1 already set up in @BeforeEach
        
        // Act
        FileMetadata saved = fileMetadataRepository.save(testFile1);
        Optional<FileMetadata> result = fileMetadataRepository.findById(saved.getId());

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getFilename()).isEqualTo("test-document.pdf");
        assertThat(result.get().getAuthor()).isEqualTo("John Doe");
        assertThat(result.get().getSize()).isEqualTo(1024L);
    }

    @Test
    @DisplayName("Should find files by author")
    void findByAuthor() {
        // Arrange
        fileMetadataRepository.save(testFile1);
        fileMetadataRepository.save(testFile2);

        // Act
        List<FileMetadata> johnFiles = fileMetadataRepository.findByAuthor("John");
        List<FileMetadata> janeFiles = fileMetadataRepository.findByAuthor("Jane");

        // Assert
        assertThat(johnFiles).hasSize(1);
        assertThat(johnFiles.get(0).getAuthor()).isEqualTo("John Doe");
        
        assertThat(janeFiles).hasSize(1);
        assertThat(janeFiles.get(0).getAuthor()).isEqualTo("Jane Smith");
    }

    @Test
    @DisplayName("Should find files by file type")
    void findByFileType() {
        // Arrange
        fileMetadataRepository.save(testFile1);
        fileMetadataRepository.save(testFile2);

        // Act
        List<FileMetadata> pdfFiles = fileMetadataRepository.findByFileType("pdf");

        // Assert
        assertThat(pdfFiles).hasSize(1);
        assertThat(pdfFiles.get(0).getFileType()).contains("pdf");
    }

    @Test
    @DisplayName("Should search files by keyword in filename or author")
    void searchByKeyword() {
        // Arrange
        fileMetadataRepository.save(testFile1);
        fileMetadataRepository.save(testFile2);

        // Act
        List<FileMetadata> documentResults = fileMetadataRepository.searchByKeyword("document");
        List<FileMetadata> smithResults = fileMetadataRepository.searchByKeyword("Smith");

        // Assert
        assertThat(documentResults).hasSize(1);
        assertThat(documentResults.get(0).getFilename()).contains("document");
        
        assertThat(smithResults).hasSize(1);
        assertThat(smithResults.get(0).getAuthor()).contains("Smith");
    }

    @Test
    @DisplayName("Should find all files ordered by upload time descending")
    void findByOrderByUploadTimeDesc() throws InterruptedException {
        // Arrange
        fileMetadataRepository.save(testFile1);
        Thread.sleep(10); // Ensure different timestamps
        fileMetadataRepository.save(testFile2);

        // Act
        List<FileMetadata> files = fileMetadataRepository.findByOrderByUploadTimeDesc();

        // Assert
        assertThat(files).hasSize(2);
        assertThat(files.get(0).getUploadTime()).isAfterOrEqualTo(files.get(1).getUploadTime());
    }

    @Test
    @DisplayName("Should check if file exists by filename and author")
    void existsByFilenameAndAuthor() {
        // Arrange
        fileMetadataRepository.save(testFile1);

        // Act
        boolean exists = fileMetadataRepository.existsByFilenameAndAuthor("test-document.pdf", "John Doe");
        boolean notExists = fileMetadataRepository.existsByFilenameAndAuthor("test-document.pdf", "Other Author");

        // Assert
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should find files by exact filename and author")
    void findByFilenameAndAuthor() {
        // Arrange
        fileMetadataRepository.save(testFile1);

        // Act
        List<FileMetadata> results = fileMetadataRepository.findByFilenameAndAuthor(
                "test-document.pdf", "John Doe");

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getFilename()).isEqualTo("test-document.pdf");
        assertThat(results.get(0).getAuthor()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should handle PrePersist lifecycle callback")
    void testPrePersistCallback() {
        // Arrange
        FileMetadata newFile = new FileMetadata();
        newFile.setFilename("new-file.txt");
        newFile.setAuthor("Test Author");
        newFile.setFileType("text/plain");
        newFile.setSize(512L);
        newFile.setObjectKey("obj-key-3");
        // Not setting uploadTime and lastEdited manually

        // Act
        FileMetadata saved = fileMetadataRepository.save(newFile);

        // Assert
        assertThat(saved.getUploadTime()).isNotNull();
        assertThat(saved.getLastEdited()).isNotNull();
        assertThat(saved.getUploadTime()).isCloseTo(saved.getLastEdited(), within(100, java.time.temporal.ChronoUnit.MILLIS));
    }

    @Test
    @DisplayName("Should handle PreUpdate lifecycle callback")
    void testPreUpdateCallback() throws InterruptedException {
        // Arrange
        FileMetadata saved = fileMetadataRepository.save(testFile1);
        Instant originalLastEdited = saved.getLastEdited();
        
        Thread.sleep(10); // Ensure time difference
        
        // Act
        saved.setSummary("Updated summary");
        FileMetadata updated = fileMetadataRepository.save(saved);

        // Assert
        assertThat(updated.getLastEdited()).isAfter(originalLastEdited);
    }

    @Test
    @DisplayName("Should delete file metadata")
    void deleteFileMetadata() {
        // Arrange
        FileMetadata saved = fileMetadataRepository.save(testFile1);
        Long fileId = saved.getId();

        // Act
        fileMetadataRepository.deleteById(fileId);
        Optional<FileMetadata> result = fileMetadataRepository.findById(fileId);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list when no files match search criteria")
    void findByAuthorWhenNoMatch() {
        // Arrange
        fileMetadataRepository.save(testFile1);

        // Act
        List<FileMetadata> results = fileMetadataRepository.findByAuthor("NonExistent");

        // Assert
        assertThat(results).isEmpty();
    }
}

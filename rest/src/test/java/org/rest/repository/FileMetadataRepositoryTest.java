package org.rest.repository;

import org.junit.jupiter.api.Test;
import org.rest.model.FileMetadata;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class FileMetadataRepositoryTest {
    @Autowired
    private FileMetadataRepository fileMetadataRepository;


    @Test
    void testSaveAndFindById() {
        FileMetadata file = new FileMetadata();
        file.setFilename("test.pdf");
        file.setAuthor("Test Author");
        file.setFileType("pdf");
        file.setSize(123L);
        FileMetadata saved = fileMetadataRepository.save(file);
        assertNotNull(saved.getId());
        assertEquals("test.pdf", saved.getFilename());
        assertEquals("Test Author", saved.getAuthor());
        assertEquals("pdf", saved.getFileType());
        assertEquals(123L, saved.getSize());
    }


    @Test
    void testDelete() {
        FileMetadata file = new FileMetadata();
        file.setFilename("delete.pdf");
        file.setAuthor("Delete Author");
        file.setFileType("pdf");
        file.setSize(456L);
        FileMetadata saved = fileMetadataRepository.save(file);
        fileMetadataRepository.deleteById(saved.getId());
        assertFalse(fileMetadataRepository.findById(saved.getId()).isPresent());
    }
}

package org.rest.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rest.model.FileMetadata;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class FileMetadataRepositoryTest {
    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @BeforeEach
    void setUp() {
        fileMetadataRepository.deleteAll();
    }

    @Test
    void testSaveAndFindById() {
        // arrange (given)
        FileMetadata file = new FileMetadata();
        file.setFilename("test.pdf");
        file.setAuthor("Test Author");
        file.setFileType("PDF");
        file.setSize(123L);
        
        // act (when)
        FileMetadata saved = fileMetadataRepository.save(file);
        
        // assert (then)
        assertNotNull(saved.getId());
        assertEquals("test.pdf", saved.getFilename());
        assertEquals("Test Author", saved.getAuthor());
        assertEquals("PDF", saved.getFileType());
        assertEquals(123L, saved.getSize());
        assertNotNull(saved.getUploadTime());
        assertNotNull(saved.getLastEdited());
    }

    @Test
    void testDelete() {
        // given
        FileMetadata file = createFileMetadata("delete.pdf", "Delete Author", "PDF", 456L);
        FileMetadata saved = fileMetadataRepository.save(file);
        
        // when
        fileMetadataRepository.deleteById(saved.getId());
        
        // then
        assertFalse(fileMetadataRepository.findById(saved.getId()).isPresent());
    }

    @Test
    void testFindByAuthor() {
        // given
        fileMetadataRepository.save(createFileMetadata("doc1.pdf", "Deborah Vance", "PDF", 100L));
        fileMetadataRepository.save(createFileMetadata("doc2.pdf", "Jane Smith", "DOCX", 200L));
        
        // when (partial match)
        List<FileMetadata> results = fileMetadataRepository.findByAuthor("Deb");
        
        // then
        assertEquals(1, results.size());
        assertEquals("Deborah Vance", results.get(0).getAuthor());
    }

    @Test
    void testFindByFileType() {
        // given
        fileMetadataRepository.save(createFileMetadata("doc1.pdf", "Author1", "PDF", 100L));
        fileMetadataRepository.save(createFileMetadata("doc2.pdf", "Author2", "PDF", 200L));
        fileMetadataRepository.save(createFileMetadata("doc3.docx", "Author3", "DOCX", 300L));
        
        // when
        List<FileMetadata> results = fileMetadataRepository.findByFileType("PDF");
        
        // then
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(f -> f.getFileType().equals("PDF")));
    }

    @Test
    void testSearchByKeyword() {
        // given
        fileMetadataRepository.save(createFileMetadata("invoice.pdf", "John Doe", "PDF", 100L));
        fileMetadataRepository.save(createFileMetadata("report.pdf", "Jane Smith", "PDF", 200L));
        
        // when (search in filename)
        List<FileMetadata> results = fileMetadataRepository.searchByKeyword("invoice");
        
        // then
        assertEquals(1, results.size());
        assertEquals("invoice.pdf", results.get(0).getFilename());
    }

    @Test
    void testFindByOrderByUploadTimeDesc() throws InterruptedException {
        // given
        fileMetadataRepository.save(createFileMetadata("first.pdf", "Author1", "PDF", 100L));
        Thread.sleep(10);
        fileMetadataRepository.save(createFileMetadata("second.pdf", "Author2", "PDF", 200L));
        Thread.sleep(10);
        fileMetadataRepository.save(createFileMetadata("third.pdf", "Author3", "PDF", 300L));
        
        // when
        List<FileMetadata> results = fileMetadataRepository.findByOrderByUploadTimeDesc();
        
        // then (newest first)
        assertEquals(3, results.size());
        assertEquals("third.pdf", results.get(0).getFilename());
        assertEquals("first.pdf", results.get(2).getFilename());
    }

    // Helper methode
    private FileMetadata createFileMetadata(String filename, String author, String fileType, Long size) {
        FileMetadata file = new FileMetadata();
        file.setFilename(filename);
        file.setAuthor(author);
        file.setFileType(fileType);
        file.setSize(size);
        return file;
    }
}

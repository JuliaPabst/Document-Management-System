package org.rest.repository;

import org.rest.model.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {
    
    List<FileMetadata> findByAuthor(String author);
    
    List<FileMetadata> findByFileType(String fileType);
    
    @Query("SELECT f FROM FileMetadata f WHERE f.filename LIKE %:keyword% OR f.author LIKE %:keyword%")
    List<FileMetadata> searchByKeyword(@Param("keyword") String keyword);
    
    List<FileMetadata> findByOrderByUploadTimeDesc();
}
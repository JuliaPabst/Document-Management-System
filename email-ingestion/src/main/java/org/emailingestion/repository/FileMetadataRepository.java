package org.emailingestion.repository;

import org.emailingestion.model.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    @Query("SELECT f FROM FileMetadata f WHERE LOWER(f.author) LIKE LOWER(CONCAT('%', :author, '%'))")
    List<FileMetadata> findByAuthor(@Param("author") String author);

    @Query("SELECT f FROM FileMetadata f WHERE LOWER(f.fileType) LIKE LOWER(CONCAT('%', :fileType, '%'))")
    List<FileMetadata> findByFileType(@Param("fileType") String fileType);

    @Query("SELECT f FROM FileMetadata f WHERE LOWER(f.filename) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(f.author) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<FileMetadata> searchByKeyword(@Param("keyword") String keyword);

    List<FileMetadata> findByOrderByUploadTimeDesc();

    @Query("SELECT f FROM FileMetadata f WHERE f.filename = :filename AND f.author = :author")
    List<FileMetadata> findByFilenameAndAuthor(@Param("filename") String filename, @Param("author") String author);

    boolean existsByFilenameAndAuthor(String filename, String author);
}

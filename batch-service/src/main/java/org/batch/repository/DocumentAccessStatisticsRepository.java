package org.batch.repository;

import org.batch.model.DocumentAccessStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DocumentAccessStatisticsRepository extends JpaRepository<DocumentAccessStatistics, Long> {

    // Find statistics for a specific document on a specific date
    Optional<DocumentAccessStatistics> findByDocumentIdAndAccessDate(Long documentId, LocalDate accessDate);

    // Check if statistics already exist for a document on a specific date
    boolean existsByDocumentIdAndAccessDate(Long documentId, LocalDate accessDate);
}

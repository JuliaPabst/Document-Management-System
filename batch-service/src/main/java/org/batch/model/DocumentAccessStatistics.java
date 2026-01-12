package org.batch.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Entity representing daily access statistics per document
 */
@Entity
@Table(name = "document_access_statistics",
        uniqueConstraints = @UniqueConstraint(columnNames = {"document_id", "access_date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentAccessStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "access_date", nullable = false)
    private LocalDate accessDate;

    @Column(name = "access_count", nullable = false)
    private Integer accessCount;

    @Column(name = "source_file")
    private String sourceFile;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @PrePersist
    protected void onCreate() {
        if (processedAt == null) {
            processedAt = Instant.now();
        }
    }
}

package org.rest.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "file_metadata")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadata {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Filename is required")
    @Column(name = "filename", nullable = false)
    private String filename;
    
    @NotBlank(message = "Author is required")
    @Column(name = "author", nullable = false)
    private String author;
    
    @NotBlank(message = "File type is required")
    @Column(name = "file_type", nullable = false)
    private String fileType;
    
    @NotNull(message = "File size is required")
    @Positive(message = "File size must be positive")
    @Column(name = "size", nullable = false)
    private Long size;
    
    @NotBlank(message = "Object key is required")
    @Column(name = "object_key", nullable = false, unique = true)
    private String objectKey;
    
    @Column(name = "upload_time", nullable = false)
    private Instant uploadTime;
    
    @Column(name = "last_edited", nullable = false)
    private Instant lastEdited;
    
    @Column(name = "summary", length = 5000)
    private String summary;
    
    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.uploadTime = now;
        this.lastEdited = now;
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.lastEdited = Instant.now();
    }
}
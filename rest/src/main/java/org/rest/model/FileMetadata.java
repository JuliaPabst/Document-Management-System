package org.rest.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    @Column(nullable = false)
    private String filename;
    
    @NotBlank(message = "Author is required")
    @Column(nullable = false)
    private String author;
    
    @NotBlank(message = "File type is required")
    @Column(nullable = false)
    private String fileType;
    
    @NotNull(message = "File size is required")
    @Positive(message = "File size must be positive")
    @Column(nullable = false)
    private Long size;
    
    @Column(nullable = false)
    private LocalDateTime uploadTime;
    
    @Column(nullable = false)
    private LocalDateTime lastEdited;
    
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.uploadTime = now;
        this.lastEdited = now;
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.lastEdited = LocalDateTime.now();
    }
}
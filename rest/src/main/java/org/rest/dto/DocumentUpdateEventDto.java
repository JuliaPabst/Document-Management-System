package org.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Event DTO for notifying about document updates or deletions 
 * (e.g. to search service for index synchronization)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUpdateEventDto implements Serializable {
    private Long documentId;
    private String filename;
    private String author;
    private String fileType;
    private Long size;
    private String objectKey;
    private String summary;
    private String extractedText;
    private EventType eventType; // UPDATE or DELETE
    
    public enum EventType {
        UPDATE,
        DELETE
    }
}

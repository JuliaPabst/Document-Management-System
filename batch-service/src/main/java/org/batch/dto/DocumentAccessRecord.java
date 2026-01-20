package org.batch.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * XML element representing a single document's access statistics
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentAccessRecord {

    @JacksonXmlProperty(localName = "DocumentId")
    private Long documentId;

    @JacksonXmlProperty(localName = "AccessCount")
    private Integer accessCount;

    @JacksonXmlProperty(localName = "LastAccessTime")
    private String lastAccessTime; 
}

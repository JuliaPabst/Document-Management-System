package org.batch.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * XML root element for access log file
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JacksonXmlRootElement(localName = "AccessLogReport")
public class AccessLogReport {

    @JacksonXmlProperty(localName = "ReportDate")
    private String reportDate; // Format: YYYY-MM-DD

    @JacksonXmlProperty(localName = "System")
    private String system;

    @JacksonXmlProperty(localName = "GeneratedAt")
    private String generatedAt;

    @JacksonXmlElementWrapper(localName = "DocumentAccesses")
    @JacksonXmlProperty(localName = "DocumentAccess")
    private List<DocumentAccessRecord> documentAccesses;
}

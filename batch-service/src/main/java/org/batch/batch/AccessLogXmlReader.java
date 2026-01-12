package org.batch.batch;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import org.batch.dto.AccessLogReport;
import org.batch.dto.DocumentAccessRecord;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * ItemReader for reading XML files and extracting DocumentAccessRecord items
 */
@Slf4j
@Component
public class AccessLogXmlReader implements ItemReader<DocumentAccessRecord> {

    @Value("${batch.input.folder}")
    private String inputFolder;

    @Value("${batch.file.pattern}")
    private String filePattern;

    private final XmlMapper xmlMapper = new XmlMapper();
    
    private List<DocumentAccessRecord> currentBatch = new ArrayList<>();
    private int currentIndex = 0;
    private boolean initialized = false;
    private String currentFileName;

    @Override
    public DocumentAccessRecord read() throws Exception {
        if (!initialized) {
            loadAllRecords();
            initialized = true;
        }

        if (currentIndex < currentBatch.size()) {
            return currentBatch.get(currentIndex++);
        }

        return null; // End of data
    }

    private void loadAllRecords() throws Exception {
        Path inputPath = Paths.get(inputFolder);
        
        if (!Files.exists(inputPath)) {
            log.warn("Input folder does not exist: {}. Creating it...", inputFolder);
            Files.createDirectories(inputPath);
            return;
        }

        log.info("Scanning input folder: {} for pattern: {}", inputFolder, filePattern);
        
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputPath, filePattern)) {
            for (Path filePath : stream) {
                if (Files.isRegularFile(filePath)) {
                    try {
                        processXmlFile(filePath.toFile());
                    } catch (Exception e) {
                        log.error("Failed to process file: {}. Error: {}", filePath.getFileName(), e.getMessage(), e);
                        // Continue with next file (skip problematic ones)
                    }
                }
            }
        }

        log.info("Loaded {} access records from XML files", currentBatch.size());
    }

    private void processXmlFile(File xmlFile) throws Exception {
        log.info("Processing XML file: {}", xmlFile.getName());
        currentFileName = xmlFile.getName();
        
        AccessLogReport report = xmlMapper.readValue(xmlFile, AccessLogReport.class);
        
        if (report.getDocumentAccesses() != null) {
            log.debug("Found {} document access records in {}", 
                    report.getDocumentAccesses().size(), xmlFile.getName());
            currentBatch.addAll(report.getDocumentAccesses());
        }
    }

    public String getCurrentFileName() {
        return currentFileName;
    }

    public void reset() {
        currentBatch.clear();
        currentIndex = 0;
        initialized = false;
        currentFileName = null;
    }
}

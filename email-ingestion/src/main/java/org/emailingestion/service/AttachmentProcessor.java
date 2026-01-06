package org.emailingestion.service;

import jakarta.mail.BodyPart;
import jakarta.mail.internet.MimeUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.emailingestion.exception.DuplicateFileException;
import org.emailingestion.model.FileMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentProcessor {

    private final FileMetadataService fileMetadataService;
    private final FileStorage fileStorage;

    @Value("${email.allowed-extensions:pdf,doc,docx,txt,png,jpg,jpeg,gif,xls,xlsx,ppt,pptx}")
    private String allowedExtensionsString;

    @Value("${email.max-file-size:52428800}")
    private long maxFileSize;

    public void processAttachment(BodyPart bodyPart, String author) {
        String filename = null;
        try {
            // decode filename
            String rawFilename = bodyPart.getFileName();
            if (rawFilename != null) {
                try {
                    filename = MimeUtility.decodeText(rawFilename);
                } catch (Exception e) {
                    log.warn("Error decoding filename '{}', using raw value", rawFilename);
                    filename = rawFilename;
                }
            }

            if (filename == null || filename.isBlank()) {
                log.warn("Attachment has no filename, skipping");
                return;
            }

            log.info("Processing attachment: {} from author: {}", filename, author);

            // Validate file extension 
            if (!isValidFileType(filename)) {
                log.warn("Rejected attachment '{}' - invalid file extension (allowed: {})",
                        filename, allowedExtensionsString);
                return;
            }

            // Read file bytes
            InputStream inputStream = bodyPart.getInputStream();
            byte[] fileBytes = inputStream.readAllBytes();

            // Validate file size
            if (fileBytes.length > maxFileSize) {
                log.warn("Rejected attachment '{}' - file size {} bytes exceeds limit {} bytes",
                        filename, fileBytes.length, maxFileSize);
                return;
            }

            if (fileBytes.length == 0) {
                log.warn("Rejected attachment '{}' - file is empty", filename);
                return;
            }

            log.info("Attachment validated - filename: {}, size: {} bytes, type: {}",
                    filename, fileBytes.length, bodyPart.getContentType());

            // Generate unique object key (timestamp + filename)
            String objectKey = System.currentTimeMillis() + "-" + filename;

            // Upload to MinIO
            String contentType = bodyPart.getContentType();
            if (contentType != null && contentType.contains(";")) {
                // Remove parameters like "charset=UTF-8"
                contentType = contentType.split(";")[0].trim();
            }
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }

            log.info("Uploading file to MinIO - key: {}, contentType: {}", objectKey, contentType);
            fileStorage.upload(objectKey, fileBytes, contentType);
            log.info("File uploaded successfully to MinIO");

            // Create FileMetadata entity
            FileMetadata fileMetadata = FileMetadata.builder()
                    .filename(filename)
                    .author(author)
                    .fileType(getFileExtension(filename))
                    .size((long) fileBytes.length)
                    .objectKey(objectKey)
                    .uploadTime(Instant.now())
                    .build();

            // CRITICAL: This triggers the entire document processing pipeline
            log.info("Saving file metadata and triggering OCR/GenAI processing pipeline");
            fileMetadataService.createFileMetadataWithWorkerNotification(fileMetadata);

            log.info("Successfully processed attachment '{}' from '{}' - ID: {}, ObjectKey: {}",
                    filename, author, fileMetadata.getId(), objectKey);

        } catch (DuplicateFileException e) {
            log.warn("Duplicate file detected: {} from author: {} - skipping", filename, author);
        } catch (Exception e) {
            log.error("Failed to process attachment '{}' from '{}': {}", filename, author, e.getMessage(), e);
        }
    }

    private boolean isValidFileType(String filename) {
        if (filename == null || filename.isBlank()) {
            return false;
        }

        String extension = getFileExtension(filename).toLowerCase();
        List<String> allowedExtensions = Arrays.asList(allowedExtensionsString.toLowerCase().split(","));

        boolean isValid = allowedExtensions.contains(extension);
        
        // Debug Log (jetzt sollte hier der saubere Dateiname stehen)
        if (!isValid) {
             log.debug("File extension validation failed - filename: {}, extension: {}, allowed: {}", 
                       filename, extension, allowedExtensions);
        }

        return isValid;
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toUpperCase();
    }
}
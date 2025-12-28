package org.emailingestion.service;

import jakarta.mail.internet.MimeBodyPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentProcessorTest {

    @Mock
    private FileMetadataService fileMetadataService;

    @Mock
    private FileStorage fileStorage;

    @InjectMocks
    private AttachmentProcessor attachmentProcessor;

    @BeforeEach
    void setUp() {
        // Set default configuration values
        ReflectionTestUtils.setField(attachmentProcessor, "allowedExtensionsString",
                "pdf,doc,docx,txt,png,jpg,jpeg,gif,xls,xlsx,ppt,pptx,csv,zip");
        ReflectionTestUtils.setField(attachmentProcessor, "maxFileSize", 52428800L); // 50MB
    }



    @Test
    void processAttachment_WithInvalidExtension_ShouldNotProcess() throws Exception {
        // Arrange
        MimeBodyPart bodyPart = new MimeBodyPart();
        bodyPart.setContent("executable content".getBytes(), "application/exe");
        bodyPart.setFileName("malware.exe");

        // Act
        attachmentProcessor.processAttachment(bodyPart, "test@example.com");

        // Assert
        verify(fileStorage, never()).upload(any(), any(), any());
        verify(fileMetadataService, never()).createFileMetadataWithWorkerNotification(any());
    }

    @Test
    void processAttachment_WithFileTooLarge_ShouldNotProcess() throws Exception {
        // Arrange
        MimeBodyPart bodyPart = new MimeBodyPart();
        byte[] largeContent = new byte[52428801]; // 50MB + 1 byte
        bodyPart.setContent(largeContent, "application/pdf");
        bodyPart.setFileName("too-large.pdf");

        // Act
        attachmentProcessor.processAttachment(bodyPart, "test@example.com");

        // Assert
        verify(fileStorage, never()).upload(any(), any(), any());
        verify(fileMetadataService, never()).createFileMetadataWithWorkerNotification(any());
    }

    @Test
    void processAttachment_WithEmptyFile_ShouldNotProcess() throws Exception {
        // Arrange
        MimeBodyPart bodyPart = new MimeBodyPart();
        bodyPart.setContent(new byte[0], "application/pdf");
        bodyPart.setFileName("empty.pdf");

        // Act
        attachmentProcessor.processAttachment(bodyPart, "test@example.com");

        // Assert
        verify(fileStorage, never()).upload(any(), any(), any());
        verify(fileMetadataService, never()).createFileMetadataWithWorkerNotification(any());
    }

    @Test
    void processAttachment_WithNoFilename_ShouldNotProcess() throws Exception {
        // Arrange
        MimeBodyPart bodyPart = new MimeBodyPart();
        bodyPart.setContent("some content".getBytes(), "application/pdf");
        // No filename set

        // Act
        attachmentProcessor.processAttachment(bodyPart, "test@example.com");

        // Assert
        verify(fileStorage, never()).upload(any(), any(), any());
        verify(fileMetadataService, never()).createFileMetadataWithWorkerNotification(any());
    }

    @Test
    void processAttachment_WithBlankFilename_ShouldNotProcess() throws Exception {
        // Arrange
        MimeBodyPart bodyPart = new MimeBodyPart();
        bodyPart.setContent("some content".getBytes(), "application/pdf");
        bodyPart.setFileName("   ");

        // Act
        attachmentProcessor.processAttachment(bodyPart, "test@example.com");

        // Assert
        verify(fileStorage, never()).upload(any(), any(), any());
        verify(fileMetadataService, never()).createFileMetadataWithWorkerNotification(any());
    }
}

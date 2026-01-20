package org.emailingestion.integration;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.emailingestion.service.EmailPollingService;
import org.emailingestion.service.FileMetadataService;
import org.emailingestion.service.FileStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@SpringBootTest
@ActiveProfiles("test")
class EmailIngestionIntegrationTest {

    @Autowired
    private EmailPollingService emailPollingService;

    @MockitoBean
    private FileStorage fileStorage;

    @MockitoBean
    private FileMetadataService fileMetadataService;

    private Session session;

    @BeforeEach
    void setUp() {
        Properties props = new Properties();
        session = Session.getInstance(props);
        reset(fileStorage, fileMetadataService);
    }

    @Test
    void fullEmailProcessingFlow_WithPdfAttachment_ShouldStoreInMinioAndDatabase() throws Exception {
        // Arrange - Create a realistic email with PDF attachment
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("john.doe@example.com"));
        message.setSubject("Important Document");

        MimeMultipart multipart = new MimeMultipart();

        // Email body
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("Please find the attached document for review.");
        multipart.addBodyPart(textPart);

        // PDF attachment
        MimeBodyPart pdfAttachment = new MimeBodyPart();
        byte[] pdfContent = "Mock PDF content - in real scenario this would be actual PDF bytes".getBytes();
        pdfAttachment.setContent(pdfContent, "application/pdf");
        pdfAttachment.setFileName("contract-2025.pdf");
        pdfAttachment.setDisposition(jakarta.mail.Part.ATTACHMENT);
        multipart.addBodyPart(pdfAttachment);

        message.setContent(multipart);
        message.saveChanges();

        // Convert to byte array (simulating what Spring Integration does)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.writeTo(baos);
        byte[] rawEmailData = baos.toByteArray();

        // Act - Process the email
        emailPollingService.handleEmailMessage(rawEmailData);

        // Assert - Verify full flow
        // 1. File should be uploaded to MinIO
        verify(fileStorage, times(1)).upload(
                argThat(objectKey -> objectKey.endsWith("-contract-2025.pdf")),
                eq(pdfContent),
                eq("application/pdf")
        );

        // 2. Metadata should be saved to database and sent to RabbitMQ
        verify(fileMetadataService, times(1)).createFileMetadataWithWorkerNotification(
                argThat(metadata ->
                        metadata.getFilename().equals("contract-2025.pdf") &&
                                metadata.getAuthor().equals("john.doe@example.com") &&
                                metadata.getFileType().equals("PDF") &&
                                metadata.getSize() == pdfContent.length
                )
        );
    }

    @Test
    void fullEmailProcessingFlow_WithMultipleAttachments_ShouldProcessAll() throws Exception {
        // Arrange
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("sender@company.com"));
        message.setSubject("Monthly Reports");

        MimeMultipart multipart = new MimeMultipart();

        // Email body
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("Here are the monthly reports.");
        multipart.addBodyPart(textPart);

        // First attachment - PDF
        MimeBodyPart pdf = new MimeBodyPart();
        byte[] pdfContent = "PDF report content".getBytes();
        pdf.setContent(pdfContent, "application/pdf");
        pdf.setFileName("report.pdf");
        pdf.setDisposition(jakarta.mail.Part.ATTACHMENT);
        multipart.addBodyPart(pdf);

        // Second attachment - Excel
        MimeBodyPart excel = new MimeBodyPart();
        byte[] excelContent = "Excel spreadsheet content".getBytes();
        excel.setContent(excelContent, "application/vnd.ms-excel");
        excel.setFileName("data.xlsx");
        excel.setDisposition(jakarta.mail.Part.ATTACHMENT);
        multipart.addBodyPart(excel);

        // Third attachment - Image
        MimeBodyPart image = new MimeBodyPart();
        byte[] imageContent = "PNG image content".getBytes();
        image.setContent(imageContent, "image/png");
        image.setFileName("chart.png");
        image.setDisposition(jakarta.mail.Part.ATTACHMENT);
        multipart.addBodyPart(image);

        message.setContent(multipart);
        message.saveChanges();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.writeTo(baos);
        byte[] rawEmailData = baos.toByteArray();

        // Act
        emailPollingService.handleEmailMessage(rawEmailData);

        // Assert - All three attachments should be processed
        verify(fileStorage, times(3)).upload(any(), any(), any());
        verify(fileMetadataService, times(3)).createFileMetadataWithWorkerNotification(any());
    }

    @Test
    void fullEmailProcessingFlow_WithInvalidAttachment_ShouldSkipInvalid() throws Exception {
        // Arrange
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("user@test.com"));
        message.setSubject("Mixed Attachments");

        MimeMultipart multipart = new MimeMultipart();

        // Valid PDF
        MimeBodyPart validPdf = new MimeBodyPart();
        validPdf.setContent("valid pdf".getBytes(), "application/pdf");
        validPdf.setFileName("valid.pdf");
        validPdf.setDisposition(jakarta.mail.Part.ATTACHMENT);
        multipart.addBodyPart(validPdf);

        // Invalid file type
        MimeBodyPart invalid = new MimeBodyPart();
        invalid.setContent("executable".getBytes(), "application/exe");
        invalid.setFileName("malware.exe");
        invalid.setDisposition(jakarta.mail.Part.ATTACHMENT);
        multipart.addBodyPart(invalid);

        message.setContent(multipart);
        message.saveChanges();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.writeTo(baos);
        byte[] rawEmailData = baos.toByteArray();

        // Act
        emailPollingService.handleEmailMessage(rawEmailData);

        // Assert - Only valid PDF should be processed
        verify(fileStorage, times(1)).upload(any(), any(), any());
        verify(fileMetadataService, times(1)).createFileMetadataWithWorkerNotification(any());
    }

    @Test
    void fullEmailProcessingFlow_PlainTextEmail_ShouldNotProcessAnything() throws Exception {
        // Arrange
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("plain@test.com"));
        message.setSubject("Plain Text Only");
        message.setText("This is just a plain text email with no attachments.");
        message.saveChanges();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.writeTo(baos);
        byte[] rawEmailData = baos.toByteArray();

        // Act
        emailPollingService.handleEmailMessage(rawEmailData);

        // Assert - Nothing should be processed
        verify(fileStorage, never()).upload(any(), any(), any());
        verify(fileMetadataService, never()).createFileMetadataWithWorkerNotification(any());
    }

    @Test
    void fullEmailProcessingFlow_EmailWithInlineImage_ShouldProcessInline() throws Exception {
        // Arrange
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("newsletter@company.com"));
        message.setSubject("Newsletter with Image");

        MimeMultipart multipart = new MimeMultipart();

        // HTML body
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent("<html><body><h1>Newsletter</h1><img src='cid:logo'/></body></html>", "text/html");
        multipart.addBodyPart(htmlPart);

        // Inline image
        MimeBodyPart inlineImage = new MimeBodyPart();
        byte[] imageContent = "logo image data".getBytes();
        inlineImage.setContent(imageContent, "image/png");
        inlineImage.setFileName("logo.png");
        inlineImage.setDisposition(jakarta.mail.Part.INLINE);
        inlineImage.setHeader("Content-ID", "<logo>");
        multipart.addBodyPart(inlineImage);

        message.setContent(multipart);
        message.saveChanges();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.writeTo(baos);
        byte[] rawEmailData = baos.toByteArray();

        // Act
        emailPollingService.handleEmailMessage(rawEmailData);

        // Assert - Inline image should be processed
        verify(fileStorage, times(1)).upload(any(), eq(imageContent), eq("image/png"));
        verify(fileMetadataService, times(1)).createFileMetadataWithWorkerNotification(
                argThat(metadata ->
                        metadata.getFilename().equals("logo.png") &&
                                metadata.getFileType().equals("PNG")
                )
        );
    }
}

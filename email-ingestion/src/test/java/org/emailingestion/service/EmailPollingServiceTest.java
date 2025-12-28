package org.emailingestion.service;

import jakarta.mail.BodyPart;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailPollingServiceTest {

    @Mock
    private AttachmentProcessor attachmentProcessor;

    @InjectMocks
    private EmailPollingService emailPollingService;

    private Session session;

    @BeforeEach
    void setUp() {
        Properties props = new Properties();
        session = Session.getInstance(props);
    }

    @Test
    void handleEmailMessage_WithPdfAttachment_ShouldProcessAttachment() throws Exception {
        // Arrange
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("test@example.com"));
        message.setSubject("Test Email with PDF");

        MimeMultipart multipart = new MimeMultipart();

        // Add text body
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("This is the email body");
        multipart.addBodyPart(textPart);

        // Add PDF attachment
        MimeBodyPart attachmentPart = new MimeBodyPart();
        attachmentPart.setContent("fake pdf content".getBytes(), "application/pdf");
        attachmentPart.setFileName("test-document.pdf");
        attachmentPart.setDisposition(jakarta.mail.Part.ATTACHMENT);
        multipart.addBodyPart(attachmentPart);

        message.setContent(multipart);
        message.saveChanges();

        // Convert to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.writeTo(baos);
        byte[] rawEmailData = baos.toByteArray();

        // Act
        emailPollingService.handleEmailMessage(rawEmailData);

        // Assert
        ArgumentCaptor<BodyPart> bodyPartCaptor = ArgumentCaptor.forClass(BodyPart.class);
        ArgumentCaptor<String> authorCaptor = ArgumentCaptor.forClass(String.class);

        verify(attachmentProcessor, times(1)).processAttachment(bodyPartCaptor.capture(), authorCaptor.capture());

        assertThat(authorCaptor.getValue()).isEqualTo("test@example.com");
        assertThat(bodyPartCaptor.getValue().getFileName()).isEqualTo("test-document.pdf");
    }

    @Test
    void handleEmailMessage_WithMultipleAttachments_ShouldProcessAllAttachments() throws Exception {
        // Arrange
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("sender@test.com"));
        message.setSubject("Multiple Attachments");

        MimeMultipart multipart = new MimeMultipart();

        // Add text body
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("Email with multiple attachments");
        multipart.addBodyPart(textPart);

        // Add first attachment
        MimeBodyPart attachment1 = new MimeBodyPart();
        attachment1.setContent("pdf content".getBytes(), "application/pdf");
        attachment1.setFileName("document1.pdf");
        attachment1.setDisposition(jakarta.mail.Part.ATTACHMENT);
        multipart.addBodyPart(attachment1);

        // Add second attachment
        MimeBodyPart attachment2 = new MimeBodyPart();
        attachment2.setContent("image content".getBytes(), "image/png");
        attachment2.setFileName("image1.png");
        attachment2.setDisposition(jakarta.mail.Part.ATTACHMENT);
        multipart.addBodyPart(attachment2);

        // Add third attachment
        MimeBodyPart attachment3 = new MimeBodyPart();
        attachment3.setContent("doc content".getBytes(), "application/msword");
        attachment3.setFileName("document.doc");
        attachment3.setDisposition(jakarta.mail.Part.ATTACHMENT);
        multipart.addBodyPart(attachment3);

        message.setContent(multipart);
        message.saveChanges();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.writeTo(baos);
        byte[] rawEmailData = baos.toByteArray();

        // Act
        emailPollingService.handleEmailMessage(rawEmailData);

        // Assert
        verify(attachmentProcessor, times(3)).processAttachment(any(BodyPart.class), eq("sender@test.com"));
    }

    @Test
    void handleEmailMessage_WithInlineAttachment_ShouldProcessInlineAttachment() throws Exception {
        // Arrange
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("inline@test.com"));
        message.setSubject("Inline Image");

        MimeMultipart multipart = new MimeMultipart();

        // Add text body
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("Email with inline image");
        multipart.addBodyPart(textPart);

        // Add inline attachment
        MimeBodyPart inlineAttachment = new MimeBodyPart();
        inlineAttachment.setContent("inline image data".getBytes(), "image/jpeg");
        inlineAttachment.setFileName("inline-image.jpg");
        inlineAttachment.setDisposition(jakarta.mail.Part.INLINE);
        multipart.addBodyPart(inlineAttachment);

        message.setContent(multipart);
        message.saveChanges();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.writeTo(baos);
        byte[] rawEmailData = baos.toByteArray();

        // Act
        emailPollingService.handleEmailMessage(rawEmailData);

        // Assert
        verify(attachmentProcessor, times(1)).processAttachment(any(BodyPart.class), eq("inline@test.com"));
    }

    @Test
    void handleEmailMessage_WithPlainText_ShouldNotProcessAttachments() throws Exception {
        // Arrange
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("plain@test.com"));
        message.setSubject("Plain Text Email");
        message.setText("This is a plain text email without attachments");
        message.saveChanges();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.writeTo(baos);
        byte[] rawEmailData = baos.toByteArray();

        // Act
        emailPollingService.handleEmailMessage(rawEmailData);

        // Assert
        verify(attachmentProcessor, never()).processAttachment(any(), any());
    }

    @Test
    void handleEmailMessage_WithNoSubject_ShouldStillProcessAttachment() throws Exception {
        // Arrange
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("nosubject@test.com"));
        // No subject set

        MimeMultipart multipart = new MimeMultipart();

        MimeBodyPart attachment = new MimeBodyPart();
        attachment.setContent("pdf data".getBytes(), "application/pdf");
        attachment.setFileName("no-subject.pdf");
        attachment.setDisposition(jakarta.mail.Part.ATTACHMENT);
        multipart.addBodyPart(attachment);

        message.setContent(multipart);
        message.saveChanges();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.writeTo(baos);
        byte[] rawEmailData = baos.toByteArray();

        // Act
        emailPollingService.handleEmailMessage(rawEmailData);

        // Assert
        verify(attachmentProcessor, times(1)).processAttachment(any(BodyPart.class), eq("nosubject@test.com"));
    }

    @Test
    void handleEmailMessage_WithMissingFromAddress_ShouldUseDefaultEmail() throws Exception {
        // Arrange
        MimeMessage message = new MimeMessage(session);
        // No from address set
        message.setSubject("No From Address");

        MimeMultipart multipart = new MimeMultipart();

        MimeBodyPart attachment = new MimeBodyPart();
        attachment.setContent("data".getBytes(), "application/pdf");
        attachment.setFileName("test.pdf");
        attachment.setDisposition(jakarta.mail.Part.ATTACHMENT);
        multipart.addBodyPart(attachment);

        message.setContent(multipart);
        message.saveChanges();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.writeTo(baos);
        byte[] rawEmailData = baos.toByteArray();

        // Act
        emailPollingService.handleEmailMessage(rawEmailData);

        // Assert
        verify(attachmentProcessor, times(1)).processAttachment(any(BodyPart.class), eq("unknown@email.com"));
    }

    @Test
    void handleEmailMessage_WithAttachmentWithoutDisposition_ShouldStillProcess() throws Exception {
        // Arrange
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("test@example.com"));
        message.setSubject("Attachment without disposition");

        MimeMultipart multipart = new MimeMultipart();

        // Add attachment without explicit disposition
        MimeBodyPart attachment = new MimeBodyPart();
        attachment.setContent("pdf data".getBytes(), "application/pdf");
        attachment.setFileName("nodisposition.pdf");
        // No disposition set - some email clients do this
        multipart.addBodyPart(attachment);

        message.setContent(multipart);
        message.saveChanges();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.writeTo(baos);
        byte[] rawEmailData = baos.toByteArray();

        // Act
        emailPollingService.handleEmailMessage(rawEmailData);

        // Assert
        verify(attachmentProcessor, times(1)).processAttachment(any(BodyPart.class), eq("test@example.com"));
    }

    @Test
    void handleEmailMessage_WithEmptyMultipart_ShouldNotProcessAttachments() throws Exception {
        // Arrange
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("empty@test.com"));
        message.setSubject("Empty Multipart");

        MimeMultipart multipart = new MimeMultipart();
        // Only text, no attachments
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText("Just text, no attachments");
        multipart.addBodyPart(textPart);

        message.setContent(multipart);
        message.saveChanges();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.writeTo(baos);
        byte[] rawEmailData = baos.toByteArray();

        // Act
        emailPollingService.handleEmailMessage(rawEmailData);

        // Assert
        verify(attachmentProcessor, never()).processAttachment(any(), any());
    }
}

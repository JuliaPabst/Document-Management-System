package org.emailingestion.service;

import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Properties;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailPollingService {

    private final AttachmentProcessor attachmentProcessor;

    /**
     * Receives the raw email bytes.
     * This guarantees that we are fully disconnected from the IMAP server.
     */
    @ServiceActivator
    public void handleEmailMessage(@Payload byte[] rawEmailData) {
        try {
            // 1. Reconstruct MimeMessage from raw bytes in memory
            Session session = Session.getDefaultInstance(new Properties());
            MimeMessage message = new MimeMessage(session, new ByteArrayInputStream(rawEmailData));

            log.info("========================================");
            log.info("New email received (reconstructed from {} bytes)", rawEmailData.length);
            log.info("========================================");

            // 2. Safe to access headers now
            String author = extractEmailAddress(message.getFrom());
            log.info("Email from: {}", author);
            log.info("Subject: {}", message.getSubject());

            int attachmentCount = 0;

            // 3. Safe to access content
            Object content = message.getContent();
            
            if (content instanceof Multipart multipart) {
                log.info("Email contains {} parts", multipart.getCount());

                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    String disposition = bodyPart.getDisposition();

                    // Debug log to see structure
                    log.debug("Part {}: type={}, disposition={}", i, bodyPart.getContentType(), disposition);

                    if (Part.ATTACHMENT.equalsIgnoreCase(disposition) ||
                            (disposition != null && disposition.equalsIgnoreCase("inline") && bodyPart.getFileName() != null)) {
                        
                        attachmentCount++;
                        log.info("Processing attachment {}: {}", attachmentCount, bodyPart.getFileName());
                        attachmentProcessor.processAttachment(bodyPart, author);
                    }
                }
            } else if (content instanceof String) {
                log.info("Email is plain text with no attachments");
            }

            log.info("Email processing completed - {} attachment(s) processed", attachmentCount);
            log.info("========================================");

        } catch (Exception e) {
            log.error("Error processing email message", e);
        }
    }

    private String extractEmailAddress(Address[] addresses) {
        if (addresses == null || addresses.length == 0) return "unknown@email.com";
        try {
            if (addresses[0] instanceof InternetAddress internetAddress) {
                return internetAddress.getAddress();
            }
            return addresses[0].toString();
        } catch (Exception e) {
            return "unknown@email.com";
        }
    }
}
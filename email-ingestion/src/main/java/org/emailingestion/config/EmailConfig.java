package org.emailingestion.config;

import jakarta.mail.Flags;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.FlagTerm;
import lombok.extern.slf4j.Slf4j;
import org.emailingestion.service.EmailPollingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.mail.dsl.Mail;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

@Configuration
@Slf4j
public class EmailConfig {

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${spring.mail.protocol}")
    private String protocol;

    @Value("${email.polling.interval}")
    private long pollingInterval;

    @Value("${email.polling.folder}")
    private String folder;

    @Value("${email.polling.mark-as-read}")
    private boolean markAsRead;

    @Value("${email.polling.delete-after-processing}")
    private boolean deleteAfterProcessing;

    @Value("${email.polling.enabled:true}")
    private boolean pollingEnabled;

    @Bean
    public IntegrationFlow emailInboundFlow(EmailPollingService emailPollingService) {
        if (!pollingEnabled) {
            log.warn("Email polling is DISABLED - no emails will be processed");
            return null;
        }

        log.info("Configuring email polling - host: {}, port: {}, user: {}, protocol: {}, folder: {}, interval: {}ms",
                host, port, username, protocol, folder, pollingInterval);

        return IntegrationFlow
                .from(Mail.imapInboundAdapter(buildEmailUrl())
                                .shouldDeleteMessages(deleteAfterProcessing)
                                .shouldMarkMessagesAsRead(markAsRead)
                                .javaMailProperties(javaMailProperties())
                                .searchTermStrategy((supportedFlags, folder) -> new FlagTerm(new Flags(Flags.Flag.SEEN), false)),
                        e -> e.poller(Pollers.fixedDelay(pollingInterval)))

                .transform(source -> {
                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ((MimeMessage) source).writeTo(baos);
                        return baos.toByteArray();
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to download email content", e);
                    }
                })

                .handle(emailPollingService, "handleEmailMessage")
                .get();
    }

    private String buildEmailUrl() {
        try {
            // URL encoding
            String encodedUsername = java.net.URLEncoder.encode(username, java.nio.charset.StandardCharsets.UTF_8);
            String encodedPassword = java.net.URLEncoder.encode(password, java.nio.charset.StandardCharsets.UTF_8);
            
            String url = String.format("%s://%s:%s@%s:%d/%s",
                    protocol, encodedUsername, encodedPassword, host, port, folder);

            log.info("Email URL configured (password hidden): {}://{}:****@{}:{}/{}",
                    protocol, username, host, port, folder);
            
            return url;
        } catch (Exception e) {
            throw new RuntimeException("Error encoding email credentials", e);
        }
    }

    private Properties javaMailProperties() {
        Properties props = new Properties();

        if (protocol.equalsIgnoreCase("imaps")) {
            props.setProperty("mail.imaps.ssl.enable", "true");
            props.setProperty("mail.imaps.ssl.trust", "*");
            props.setProperty("mail.imaps.auth", "true");
            props.setProperty("mail.imaps.connectiontimeout", "10000");
            props.setProperty("mail.imaps.timeout", "10000");
        } else if (protocol.equalsIgnoreCase("imap")) {
            props.setProperty("mail.imap.auth", "true");
            props.setProperty("mail.imap.connectiontimeout", "10000");
            props.setProperty("mail.imap.timeout", "10000");
        }

        props.setProperty("mail.debug", "false");

        return props;
    }
}
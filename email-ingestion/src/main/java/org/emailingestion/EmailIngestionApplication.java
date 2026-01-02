package org.emailingestion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.integration.config.EnableIntegration;

@SpringBootApplication
@EnableIntegration
@Slf4j
public class EmailIngestionApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmailIngestionApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("========================================");
        log.info("Email Ingestion Service Started");
        log.info("========================================");
        log.info("The service is now monitoring the configured email account for new documents.");
        log.info("Documents received as email attachments will be automatically processed.");
        log.info("Processing pipeline: Email → MinIO → Database → RabbitMQ → OCR → GenAI");
        log.info("========================================");
    }
}

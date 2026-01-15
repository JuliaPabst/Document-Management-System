package org.rest.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Additional test configuration for tests that need the OCR Worker
 * Only imported by tests that specifically test OCR functionality
 * 
 * Usage: @Import({TestcontainersConfiguration.class, OcrWorkerTestConfiguration.class})
 */
@TestConfiguration(proxyBeanMethods = false)
public class OcrWorkerTestConfiguration {

    /**
     * OCR Worker container for full end-to-end testing
     * 
     * Prerequisites:
     * 1. Build the OCR worker Docker image:
     *    cd paperlessWorkers && docker build -t paperless-workers:latest .
     */
    @Bean
    @SuppressWarnings("resource")
    GenericContainer<?> ocrWorkerContainer(
            Network network,
            RabbitMQContainer rabbitMQContainer,
            GenericContainer<?> minioContainer) {
        
        return new GenericContainer<>(DockerImageName.parse("paperless-workers:latest"))
                .withNetwork(network)
                .withEnv("RABBITMQ_HOST", "rabbitmq")
                .withEnv("RABBITMQ_PORT", "5672")
                .withEnv("RABBITMQ_USERNAME", rabbitMQContainer.getAdminUsername())
                .withEnv("RABBITMQ_PASSWORD", rabbitMQContainer.getAdminPassword())
                .withEnv("MINIO_ENDPOINT", "minio")
                .withEnv("MINIO_PORT", "9000")
                .withEnv("MINIO_ACCESS_KEY", "minioadmin")
                .withEnv("MINIO_SECRET_KEY", "minioadmin")
                .withEnv("MINIO_BUCKET_NAME", "test-documents")
                .waitingFor(Wait.forLogMessage(".*Started WorkersApplication.*", 1))
                .withStartupTimeout(java.time.Duration.ofMinutes(2));
    }
}

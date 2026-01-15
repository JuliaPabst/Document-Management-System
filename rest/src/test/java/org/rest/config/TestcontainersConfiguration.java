package org.rest.config;

import org.springframework.amqp.core.Queue;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Testcontainers configuration for integration tests
 * Provides PostgreSQL, RabbitMQ, MinIO, and optionally OCR Worker containers
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    Network testNetwork() {
        return Network.newNetwork();
    }

    @Bean
    @ServiceConnection
    @SuppressWarnings("resource") // Container lifecycle managed by Spring Test
    PostgreSQLContainer<?> postgresContainer(Network network) {
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(DockerImageName.parse("postgres:14-alpine"))
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withNetwork(network)
                .withNetworkAliases("postgres");
        container.start();
        return container;
    }

    @Bean
    @ServiceConnection
    @SuppressWarnings("resource") // Container lifecycle managed by Spring Test
    RabbitMQContainer rabbitMQContainer(Network network) {
        RabbitMQContainer container = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.13-management-alpine"))
                .withNetwork(network)
                .withNetworkAliases("rabbitmq");
        container.start();
        return container;
    }

    @Bean
    @SuppressWarnings("resource") // Container lifecycle managed by Spring Test
    GenericContainer<?> minioContainer(Network network) {
        GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("minio/minio:latest"))
                .withCommand("server", "/data")
                .withEnv("MINIO_ROOT_USER", "minioadmin")
                .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
                .withExposedPorts(9000)
                .withNetwork(network)
                .withNetworkAliases("minio");
        
        // Start container immediately
        container.start();
        return container;
    }

    @Bean
    DynamicPropertyRegistrar rabbitMQPropertyRegistrar(RabbitMQContainer rabbitMQContainer) {
        return (DynamicPropertyRegistry registry) -> {
            // Override environment variable placeholders with actual container values
            registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
            registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
            registry.add("spring.rabbitmq.username", rabbitMQContainer::getAdminUsername);
            registry.add("spring.rabbitmq.password", rabbitMQContainer::getAdminPassword);
        };
    }

    @Bean
    DynamicPropertyRegistrar minioPropertyRegistrar(GenericContainer<?> minioContainer) {
        return (DynamicPropertyRegistry registry) -> {
            registry.add("minio.endpoint", minioContainer::getHost);
            registry.add("minio.port", () -> String.valueOf(minioContainer.getMappedPort(9000)));
            registry.add("minio.access-key", () -> "minioadmin");
            registry.add("minio.secret-key", () -> "minioadmin");
            registry.add("minio.bucket-name", () -> "test-documents");
            registry.add("minio.use-ssl", () -> "false");
        };
    }

    // Declare RabbitMQ queues for testing
    @Bean
    public Queue ocrQueue() {
        return new Queue("ocr-worker-queue", false);
    }

    @Bean
    public Queue genaiQueue() {
        return new Queue("genai-worker-queue", false);
    }

    @Bean
    public Queue ocrResultQueue() {
        return new Queue("ocr-result-queue", false);
    }

    @Bean
    public Queue genaiResultQueue() {
        return new Queue("genai-result-queue", false);
    }

    @Bean
    public Queue searchIndexingQueue() {
        return new Queue("search-indexing-queue", false);
    }

    /**
     * OCR Worker container - only starts when test.ocr.enabled=true
     * Tries to use existing image, otherwise builds from paperlessWorkers Dockerfile
     */
    @Bean
    @SuppressWarnings("resource") // Container lifecycle managed by Spring Test
    GenericContainer<?> ocrWorkerContainer(Network network, RabbitMQContainer rabbitMQContainer, GenericContainer<?> minioContainer) {
        boolean ocrEnabled = Boolean.parseBoolean(System.getProperty("test.ocr.enabled", "false"));
        
        if (!ocrEnabled) {
            System.out.println("ℹ️  OCR Worker container is DISABLED");
            System.out.println("   To enable, run: ./mvnw test -Dtest.ocr.enabled=true");
            return null; // No container if OCR is disabled
        }

        String imageName = "paperless-workers-test:latest";
        GenericContainer<?> container;
        
        try {
            // Try to use existing image first
            System.out.println("🔍 Checking for existing OCR Worker image: " + imageName);
            container = new GenericContainer<>(DockerImageName.parse(imageName));
            System.out.println("✅ Using existing OCR Worker image");
        } catch (Exception e) {
            // If image doesn't exist, build it from Dockerfile
            System.out.println("🔨 Building OCR Worker image (first time)");
            System.out.println("   This will take 2-3 minutes. Subsequent runs will reuse the image.");
            
            Path paperlessWorkersPath = Paths.get(System.getProperty("user.dir"))
                    .getParent()
                    .resolve("paperlessWorkers");
            
            ImageFromDockerfile ocrWorkerImage = new ImageFromDockerfile(imageName, false)
                    .withDockerfile(paperlessWorkersPath.resolve("Dockerfile"))
                    .withFileFromPath(".", paperlessWorkersPath);
            
            container = new GenericContainer<>(ocrWorkerImage);
        }

        // Configure and start the container
        container
                .withNetwork(network)
                .withNetworkAliases("ocr-worker")
                .withEnv("RABBITMQ_HOST", "rabbitmq")
                .withEnv("RABBITMQ_PORT", "5672")
                .withEnv("RABBITMQ_USERNAME", rabbitMQContainer.getAdminUsername())
                .withEnv("RABBITMQ_PASSWORD", rabbitMQContainer.getAdminPassword())
                .withEnv("MINIO_ENDPOINT", "minio")
                .withEnv("MINIO_PORT", "9000")
                .withEnv("MINIO_ACCESS_KEY", "minioadmin")
                .withEnv("MINIO_SECRET_KEY", "minioadmin")
                .withEnv("MINIO_BUCKET_NAME", "test-documents")
                .withEnv("MINIO_USE_SSL", "false")
                .withEnv("OPENAI_API_KEY", "test-key")
                .waitingFor(Wait.forLogMessage(".*Started WorkersApplication.*", 1))
                .withStartupTimeout(java.time.Duration.ofMinutes(5))
                .dependsOn(rabbitMQContainer, minioContainer);
        
        // Start the container and provide helpful error messages
        try {
            container.start();
            System.out.println("OCR Worker container started successfully");
        } catch (Exception e) {
            System.err.println("Failed to start OCR Worker container:");
            System.err.println("   " + e.getMessage());
            System.err.println("\nContainer logs:");
            try {
                System.err.println(container.getLogs());
            } catch (Exception logEx) {
                System.err.println("   Could not retrieve logs: " + logEx.getMessage());
            }
            throw new RuntimeException("OCR Worker container failed to start. " +
                    "Check the logs above for details.", e);
        }
        
        return container;
    }
}

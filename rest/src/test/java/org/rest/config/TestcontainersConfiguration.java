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
                .withNetworkAliases("rabbitmq")
                .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1))
                .withStartupTimeout(java.time.Duration.ofMinutes(2));
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
                .withNetworkAliases("minio")
                .waitingFor(Wait.forHttp("/minio/health/ready")
                        .forPort(9000)
                        .forStatusCode(200))
                .withStartupTimeout(java.time.Duration.ofMinutes(2));
        
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
            System.out.println("  OCR Worker container is DISABLED");
            System.out.println("   To enable, run: ./mvnw test -Dtest.ocr.enabled=true");
            return null; // No container if OCR is disabled
        }

        String imageName = "paperless-workers-test:latest";
        GenericContainer<?> container;
        
        try {
            // Try to use existing image first
            System.out.println(" Checking for existing OCR Worker image: " + imageName);
            container = new GenericContainer<>(DockerImageName.parse(imageName));
            System.out.println(" Using existing OCR Worker image");
        } catch (Exception e) {
            // If image doesn't exist, build it from Dockerfile
            System.out.println(" Building OCR Worker image (first time)");
            System.out.println("   This will take 2-3 minutes. Subsequent runs will reuse the image.");
            
            Path paperlessWorkersPath = Paths.get(System.getProperty("user.dir"))
                    .getParent()
                    .resolve("paperlessWorkers");
            
            ImageFromDockerfile ocrWorkerImage = new ImageFromDockerfile(imageName, false)
                    .withDockerfile(paperlessWorkersPath.resolve("Dockerfile"))
                    .withFileFromPath(".", paperlessWorkersPath);
            
            container = new GenericContainer<>(ocrWorkerImage);
        }

        // Make container effectively final for use in lambda
        final GenericContainer<?> finalContainer = container;

        // Configure the container
        finalContainer
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
                // Make RabbitMQ connection more resilient for tests
                .withEnv("SPRING_RABBITMQ_CONNECTION_TIMEOUT", "60000")
                .withEnv("SPRING_RABBITMQ_REQUESTED_HEARTBEAT", "60")
                // Don't fail on startup if RabbitMQ is temporarily unavailable
                .withEnv("SPRING_RABBITMQ_LISTENER_SIMPLE_RETRY_ENABLED", "true")
                .withEnv("SPRING_RABBITMQ_LISTENER_SIMPLE_RETRY_INITIAL_INTERVAL", "3000")
                .withEnv("SPRING_RABBITMQ_LISTENER_SIMPLE_RETRY_MAX_ATTEMPTS", "10")
                // Use a simpler wait strategy - just wait for container to stay running
                // The "Started" log message gets buried by RabbitMQ connection retry logs
                .waitingFor(Wait.forListeningPort())  // Wait for any port to be listening
                .withStartupTimeout(java.time.Duration.ofMinutes(2))  // Longer timeout for real OCR
                // Ensure dependencies are fully started before starting this container
                .dependsOn(minioContainer, rabbitMQContainer);
        
        // Give RabbitMQ and MinIO extra time to be fully ready on the network
        System.out.println(" Waiting for dependencies to be fully ready...");
        try {
            Thread.sleep(5000); // 5 second grace period to ensure queues are declared
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Start the container explicitly (dependencies are already started above)
        System.out.println(" Starting OCR Worker container...");
        
        try {
            finalContainer.start();
            System.out.println(" OCR Worker container started successfully!");
        } catch (Exception e) {
            System.err.println(" Failed to start OCR Worker container!");
            System.err.println(" Error: " + e.getMessage());
            System.err.println("\n Container logs:");
            try {
                String logs = finalContainer.getLogs();
                for (String line : logs.split("\n")) {
                    System.err.println("   " + line);
                }
            } catch (Exception logEx) {
                System.err.println(" Could not retrieve logs: " + logEx.getMessage());
            }
            throw e;
        }
        
        return finalContainer;
    }
}

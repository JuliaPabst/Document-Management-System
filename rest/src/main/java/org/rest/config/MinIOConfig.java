package org.rest.config;

import io.minio.MinioClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@Getter
public class MinIOConfig {
	@Value("${minio.endpoint}")
	private String endpoint;
	@Value("${minio.port}")
	private int port;
	@Value("${minio.access-key}")
	private String accessKey;
	@Value("${minio.secret-key}")
	private String secretKey;
	@Value("${minio.bucket-name}")
	private String bucketName;
	@Value("${minio.use-ssl}")
	private boolean useSsl;

	@Bean
	public MinioClient minioClient() {
		try {
			log.info("Initializing MinIO client - endpoint: {}:{}, bucket: {}, ssl: {}",
					endpoint, port, bucketName, useSsl);

			return MinioClient.builder()
					.endpoint(endpoint, port, useSsl)
					.credentials(accessKey, secretKey)
					.build();
		} catch (Exception e) {
			log.error("Failed to initialize MinIO client: {}", e.getMessage(), e);
			throw new RuntimeException("MinIO client initialization failed", e);
		}
	}
}
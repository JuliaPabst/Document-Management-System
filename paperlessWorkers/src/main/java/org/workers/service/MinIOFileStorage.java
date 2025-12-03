package org.workers.service;

import io.minio.*;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.workers.config.MinIOConfig;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

// Service for downloading and checking existence of files in MinIO
@Service
@RequiredArgsConstructor
@Slf4j
public class MinIOFileStorage implements FileStorage {

	private final MinioClient minioClient;
	private final MinIOConfig minioConfig;

	// Download object/file from MinIO as byte array
	@Override
	public byte[] download(String objectKey) {
		try {
			log.info("Downloading file from MinIO - bucket: {}, key: {}",
					minioConfig.getBucketName(), objectKey);

			try (InputStream stream = minioClient.getObject(
					GetObjectArgs.builder()
							.bucket(minioConfig.getBucketName())
							.object(objectKey)
							.build())) {

				// Read all bytes from the stream into memory
				byte[] content = stream.readAllBytes();
				log.info("File downloaded successfully from MinIO: {} ({} bytes)", objectKey, content.length);
				return content;
			}
		} catch (ErrorResponseException e) {
			if (e.errorResponse().code().equals("NoSuchKey")) {
				log.error("File not found in MinIO: {}", objectKey);
				throw new RuntimeException("File not found: " + objectKey);
			}
			log.error("MinIO error during download - key: {}, error: {}", objectKey, e.getMessage());
			throw new RuntimeException("Failed to download file from MinIO: " + e.getMessage(), e);
		} catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
			log.error("Error during file download - key: {}, error: {}", objectKey, e.getMessage());
			throw new RuntimeException("Failed to download file: " + e.getMessage(), e);
		}
	}

	// Check if object exists in MinIO, using statObject
	@Override
	public boolean exists(String objectKey) {
		try {
			minioClient.statObject(
					StatObjectArgs.builder()
							.bucket(minioConfig.getBucketName())
							.object(objectKey)
							.build());
			return true;
		} catch (ErrorResponseException e) {
			if (e.errorResponse().code().equals("NoSuchKey")) {
				return false;
			}
			log.error("Error checking file existence - key: {}, error: {}", objectKey, e.getMessage());
			throw new RuntimeException("Failed to check file existence: " + e.getMessage(), e);
		} catch (Exception e) {
			log.error("Error checking file existence - key: {}, error: {}", objectKey, e.getMessage());
			throw new RuntimeException("Failed to check file existence: " + e.getMessage(), e);
		}
	}
}
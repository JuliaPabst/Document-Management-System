package org.emailingestion.service;

import io.minio.*;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.emailingestion.config.MinIOConfig;
import org.emailingestion.exception.FileStorageException;
import org.emailingestion.exception.StorageFileNotFoundException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * MinIO implementation of FileStorage interface for object storage operations.
 * Handles file upload, download, deletion, and existence checks with automatic bucket creation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MinIOFileStorage implements FileStorage {

	private final MinioClient minioClient;
	private final MinIOConfig minioConfig;

	@Override
	public void upload(String objectKey, byte[] fileData, String contentType) {
		try {
			ensureBucketExists();

			log.info("Uploading file to MinIO - bucket: {}, key: {}, size: {} bytes, contentType: {}",
					minioConfig.getBucketName(), objectKey, fileData.length, contentType);

			minioClient.putObject(
					PutObjectArgs.builder()
							.bucket(minioConfig.getBucketName())
							.object(objectKey)
							.stream(new ByteArrayInputStream(fileData), fileData.length, -1)
							.contentType(contentType)
							.build());

			log.info("File uploaded successfully to MinIO: {}", objectKey);
		} catch (MinioException e) {
			log.error("MinIO error during upload - key: {}, error: {}", objectKey, e.getMessage());
			throw new FileStorageException("Failed to upload file to MinIO: " + e.getMessage(), e);
		} catch (IOException | NoSuchAlgorithmException | InvalidKeyException e) {
			log.error("Error during file upload - key: {}, error: {}", objectKey, e.getMessage());
			throw new FileStorageException("Failed to upload file: " + e.getMessage(), e);
		}
	}

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

				byte[] content = stream.readAllBytes();
				log.info("File downloaded successfully from MinIO: {} ({} bytes)", objectKey, content.length);
				return content;
			}
		} catch (ErrorResponseException e) {
			if (e.errorResponse().code().equals("NoSuchKey")) {
				log.error("File not found in MinIO: {}", objectKey);
				throw new StorageFileNotFoundException(objectKey);
			}
			log.error("MinIO error during download - key: {}, error: {}", objectKey, e.getMessage());
			throw new FileStorageException("Failed to download file from MinIO: " + e.getMessage(), e);
		} catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
			log.error("Error during file download - key: {}, error: {}", objectKey, e.getMessage());
			throw new FileStorageException("Failed to download file: " + e.getMessage(), e);
		}
	}

	@Override
	public void delete(String objectKey) {
		try {
			log.info("Deleting file from MinIO - bucket: {}, key: {}",
					minioConfig.getBucketName(), objectKey);

			minioClient.removeObject(
					RemoveObjectArgs.builder()
							.bucket(minioConfig.getBucketName())
							.object(objectKey)
							.build());

			log.info("File deleted successfully from MinIO: {}", objectKey);
		} catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
			log.error("Error during file deletion - key: {}, error: {}", objectKey, e.getMessage());
			throw new FileStorageException("Failed to delete file: " + e.getMessage(), e);
		}
	}

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
			throw new FileStorageException("Failed to check file existence: " + e.getMessage(), e);
		} catch (Exception e) {
			log.error("Error checking file existence - key: {}, error: {}", objectKey, e.getMessage());
			throw new FileStorageException("Failed to check file existence: " + e.getMessage(), e);
		}
	}

	// Ensures the configured bucket exists, creates it if not
	private void ensureBucketExists() {
		try {
			boolean bucketExists = minioClient.bucketExists(
					BucketExistsArgs.builder()
							.bucket(minioConfig.getBucketName())
							.build());

			if (!bucketExists) {
				log.info("Bucket '{}' does not exist, creating it...", minioConfig.getBucketName());
				minioClient.makeBucket(
						MakeBucketArgs.builder()
								.bucket(minioConfig.getBucketName())
								.build());
				log.info("Bucket '{}' created successfully", minioConfig.getBucketName());
			} else {
				log.debug("Bucket '{}' already exists", minioConfig.getBucketName());
			}
		} catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
			log.error("Failed to ensure bucket existence: {}", e.getMessage());
			throw new FileStorageException("Failed to ensure bucket exists: " + e.getMessage(), e);
		}
	}
}
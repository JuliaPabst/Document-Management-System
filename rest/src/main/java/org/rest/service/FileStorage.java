package org.rest.service;

/**
 * Interface for object storage operations (implemented by MinIO)
 */
public interface FileStorage {
	void upload(String objectKey, byte[] fileData, String contentType);
	byte[] download(String objectKey);
	void delete(String objectKey);
	boolean exists(String objectKey);
}
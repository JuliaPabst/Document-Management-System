package org.emailingestion.service;

/**
 * Abstraction for object storage operations (implemented by MinIOFileStorage)
 */
public interface FileStorage {
	void upload(String objectKey, byte[] fileData, String contentType);
	byte[] download(String objectKey);
	void delete(String objectKey);
	boolean exists(String objectKey);
}

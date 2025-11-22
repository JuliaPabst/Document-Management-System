package org.rest.service;

public interface FileStorage {
	void upload(String objectKey, byte[] fileData, String contentType);
	byte[] download(String objectKey);
	void delete(String objectKey);
	boolean exists(String objectKey);
}
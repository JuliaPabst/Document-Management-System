package org.emailingestion.exception;

// Exception thrown when a requested file does not exist in storage
public class StorageFileNotFoundException extends FileStorageException {
    
    public StorageFileNotFoundException(String objectKey) {
        super("File not found in storage: " + objectKey);
    }
}
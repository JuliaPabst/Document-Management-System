package org.rest.exception;

public class FileMetadataNotFoundException extends RuntimeException {
    public FileMetadataNotFoundException(String message) {
        super(message);
    }
}
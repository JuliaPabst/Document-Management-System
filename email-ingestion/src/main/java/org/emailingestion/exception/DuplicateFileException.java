package org.emailingestion.exception;

/**
 * Thrown when attempting to upload file that already exists for the same author
 */
public class DuplicateFileException extends RuntimeException {
    public DuplicateFileException(String message) {
        super(message);
    }
}

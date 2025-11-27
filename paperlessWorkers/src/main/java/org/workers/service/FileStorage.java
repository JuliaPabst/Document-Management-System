package org.workers.service;

public interface FileStorage {
    byte[] download(String objectKey);
    boolean exists(String objectKey);
}
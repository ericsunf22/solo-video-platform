package com.solo.video.exception;

public class FileStorageException extends BusinessException {
    public FileStorageException(String message) {
        super(500, message);
    }
}

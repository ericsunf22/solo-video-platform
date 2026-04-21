package com.solo.video.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;

public interface FileStorageService {
    
    String storeFile(MultipartFile file);
    
    String storeFile(MultipartFile file, String subDirectory);
    
    Path getStoragePath();
    
    Path getCoverPath();
    
    Path getTempPath();
    
    void deleteFile(String relativePath);
    
    File getFile(String relativePath);
    
    boolean fileExists(String relativePath);
    
    String generateUniqueFileName(String originalFileName);
    
    void initStorageDirectories();
}

package com.solo.video.service.impl;

import com.solo.video.config.FileStorageConfig;
import com.solo.video.exception.FileStorageException;
import com.solo.video.service.FileStorageService;
import com.solo.video.util.FileUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {
    
    private final FileStorageConfig fileStorageConfig;
    private Path storagePath;
    private Path coverPath;
    private Path tempPath;
    
    @PostConstruct
    public void init() {
        initStorageDirectories();
    }
    
    @Override
    public void initStorageDirectories() {
        try {
            storagePath = Paths.get(fileStorageConfig.getStorage().getPath()).toAbsolutePath().normalize();
            coverPath = Paths.get(fileStorageConfig.getStorage().getCoverPath()).toAbsolutePath().normalize();
            tempPath = Paths.get(fileStorageConfig.getStorage().getTempPath()).toAbsolutePath().normalize();
            
            Files.createDirectories(storagePath);
            Files.createDirectories(coverPath);
            Files.createDirectories(tempPath);
            
            log.info("存储目录初始化完成:");
            log.info("  视频存储: {}", storagePath);
            log.info("  封面存储: {}", coverPath);
            log.info("  临时文件: {}", tempPath);
        } catch (IOException e) {
            log.error("无法创建存储目录", e);
            throw new FileStorageException("无法创建存储目录: " + e.getMessage());
        }
    }
    
    @Override
    public String storeFile(MultipartFile file) {
        return storeFile(file, null);
    }
    
    @Override
    public String storeFile(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("文件不能为空");
        }
        
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFileName == null || originalFileName.isEmpty()) {
            throw new FileStorageException("文件名不能为空");
        }
        
        if (originalFileName.contains("..")) {
            throw new FileStorageException("文件名包含非法路径序列: " + originalFileName);
        }
        
        try {
            String uniqueFileName = generateUniqueFileName(originalFileName);
            Path targetPath;
            
            if (subDirectory != null && !subDirectory.isEmpty()) {
                Path subPath = storagePath.resolve(subDirectory);
                Files.createDirectories(subPath);
                targetPath = subPath.resolve(uniqueFileName);
            } else {
                String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                Path datePath = storagePath.resolve(dateDir);
                Files.createDirectories(datePath);
                targetPath = datePath.resolve(uniqueFileName);
            }
            
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            
            String relativePath = storagePath.relativize(targetPath).toString();
            log.info("文件存储成功: {} -> {}", originalFileName, relativePath);
            
            return relativePath;
        } catch (IOException e) {
            log.error("文件存储失败", e);
            throw new FileStorageException("文件存储失败: " + e.getMessage());
        }
    }
    
    @Override
    public Path getStoragePath() {
        return storagePath;
    }
    
    @Override
    public Path getCoverPath() {
        return coverPath;
    }
    
    @Override
    public Path getTempPath() {
        return tempPath;
    }
    
    @Override
    public void deleteFile(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return;
        }
        
        try {
            Path filePath = storagePath.resolve(relativePath).normalize();
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("文件已删除: {}", relativePath);
            }
        } catch (IOException e) {
            log.error("文件删除失败: {}", relativePath, e);
        }
    }
    
    @Override
    public File getFile(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return null;
        }
        
        Path filePath = storagePath.resolve(relativePath).normalize();
        return filePath.toFile();
    }
    
    @Override
    public boolean fileExists(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return false;
        }
        
        Path filePath = storagePath.resolve(relativePath).normalize();
        return Files.exists(filePath);
    }
    
    @Override
    public String generateUniqueFileName(String originalFileName) {
        String extension = FileUtil.getFileExtension(originalFileName);
        String fileNameWithoutExt = FileUtil.getFileNameWithoutExtension(originalFileName);
        
        String safeFileName = fileNameWithoutExt.replaceAll("[^a-zA-Z0-9_-]", "_");
        if (safeFileName.length() > 50) {
            safeFileName = safeFileName.substring(0, 50);
        }
        
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String timestamp = String.valueOf(System.currentTimeMillis());
        
        if (extension == null || extension.isEmpty()) {
            return String.format("%s_%s_%s", safeFileName, uniqueId, timestamp);
        }
        
        return String.format("%s_%s_%s.%s", safeFileName, uniqueId, timestamp, extension);
    }
}

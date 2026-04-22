package com.solo.video.controller;

import com.solo.video.config.FileStorageConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/api/covers")
@RequiredArgsConstructor
public class CoverController {
    
    private final FileStorageConfig fileStorageConfig;
    
    @GetMapping("/{coverFileName}")
    public void getCover(
            @PathVariable String coverFileName,
            HttpServletResponse response
    ) {
        if (!StringUtils.hasText(coverFileName)) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }
        
        try {
            Path coverPath = Paths.get(fileStorageConfig.getStorage().getCoverPath())
                    .toAbsolutePath()
                    .normalize()
                    .resolve(coverFileName)
                    .normalize();
            
            Path coverDir = Paths.get(fileStorageConfig.getStorage().getCoverPath())
                    .toAbsolutePath()
                    .normalize();
            
            if (!coverPath.startsWith(coverDir)) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                return;
            }
            
            if (!Files.exists(coverPath)) {
                response.setStatus(HttpStatus.NOT_FOUND.value());
                return;
            }
            
            String contentType = Files.probeContentType(coverPath);
            if (contentType == null) {
                contentType = MediaType.IMAGE_JPEG_VALUE;
            }
            
            response.setContentType(contentType);
            response.setStatus(HttpStatus.OK.value());
            
            try (InputStream inputStream = Files.newInputStream(coverPath);
                 OutputStream outputStream = response.getOutputStream()) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
            
        } catch (Exception e) {
            log.error("读取封面图失败: {}", coverFileName, e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
}

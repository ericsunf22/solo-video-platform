package com.solo.video.controller;

import com.solo.video.entity.Video;
import com.solo.video.service.VideoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;

@Slf4j
@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
public class StreamController {
    
    private final VideoService videoService;
    
    @Value("${app.video.stream.buffer-size:8192}")
    private int bufferSize;
    
    @GetMapping("/{videoId}")
    public void streamVideo(
            @PathVariable Long videoId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Video video = videoService.getVideoEntityById(videoId);
        
        File videoFile = getVideoFile(video);
        if (videoFile == null || !videoFile.exists()) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }
        
        String range = request.getHeader(HttpHeaders.RANGE);
        
        if (StringUtils.hasText(range)) {
            handleRangeRequest(videoFile, range, response);
        } else {
            handleFullRequest(videoFile, response, video);
        }
    }
    
    private File getVideoFile(Video video) {
        if (video.getSourceType() == Video.SourceType.SCANNED) {
            return new File(video.getFilePath());
        }
        
        Path filePath = Path.of(video.getFilePath());
        if (filePath.isAbsolute()) {
            return filePath.toFile();
        }
        
        Path storagePath = Path.of("./storage/videos");
        return storagePath.resolve(filePath).toFile();
    }
    
    private void handleRangeRequest(File file, String range, HttpServletResponse response) {
        try {
            long fileLength = file.length();
            long start = 0;
            long end = fileLength - 1;
            
            if (range.startsWith("bytes=")) {
                String rangeValue = range.substring("bytes=".length());
                int dashIndex = rangeValue.indexOf('-');
                
                if (dashIndex == 0) {
                    long suffixLength = Long.parseLong(rangeValue.substring(1));
                    start = Math.max(0, fileLength - suffixLength);
                } else if (dashIndex == rangeValue.length() - 1) {
                    start = Long.parseLong(rangeValue.substring(0, dashIndex));
                } else {
                    start = Long.parseLong(rangeValue.substring(0, dashIndex));
                    end = Long.parseLong(rangeValue.substring(dashIndex + 1));
                    end = Math.min(end, fileLength - 1);
                }
            }
            
            if (start > end || start >= fileLength) {
                response.setStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value());
                return;
            }
            
            long contentLength = end - start + 1;
            
            response.setStatus(HttpStatus.PARTIAL_CONTENT.value());
            response.setContentType(getContentType(file.getName()));
            response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
            response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLength);
            response.setContentLengthLong(contentLength);
            
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
                 FileChannel fileChannel = randomAccessFile.getChannel();
                 OutputStream outputStream = response.getOutputStream();
                 WritableByteChannel writableChannel = Channels.newChannel(outputStream)) {
                
                randomAccessFile.seek(start);
                long remaining = contentLength;
                
                while (remaining > 0) {
                    long transferred = fileChannel.transferTo(randomAccessFile.getFilePointer(), remaining, writableChannel);
                    if (transferred <= 0) {
                        break;
                    }
                    remaining -= transferred;
                }
            }
            
        } catch (Exception e) {
            log.error("处理范围请求失败", e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
    
    private void handleFullRequest(File file, HttpServletResponse response, Video video) {
        try {
            response.setStatus(HttpStatus.OK.value());
            response.setContentType(getContentType(file.getName()));
            response.setContentLengthLong(file.length());
            response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
            
            try (InputStream inputStream = new FileInputStream(file);
                 OutputStream outputStream = response.getOutputStream()) {
                
                byte[] buffer = new byte[bufferSize];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
            
        } catch (Exception e) {
            log.error("处理完整请求失败", e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
    
    private String getContentType(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase();
        
        return switch (extension) {
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            case "ogg" -> "video/ogg";
            case "mov" -> "video/quicktime";
            case "avi" -> "video/x-msvideo";
            case "wmv" -> "video/x-ms-wmv";
            case "mkv" -> "video/x-matroska";
            case "flv" -> "video/x-flv";
            case "m4v" -> "video/x-m4v";
            default -> "application/octet-stream";
        };
    }
    
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return fileName.substring(lastDot + 1);
    }
}

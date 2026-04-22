package com.solo.video.service.impl;

import com.solo.video.dto.VideoMetadata;
import com.solo.video.dto.response.ScanResultResponse;
import com.solo.video.entity.Video;
import com.solo.video.repository.VideoRepository;
import com.solo.video.service.FileScanService;
import com.solo.video.service.VideoMetadataService;
import com.solo.video.util.FileUtil;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileScanServiceImpl implements FileScanService {
    
    private final VideoRepository videoRepository;
    private final VideoMetadataService videoMetadataService;
    
    private final AtomicInteger scanProgress = new AtomicInteger(0);
    private final AtomicBoolean isScanning = new AtomicBoolean(false);
    private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
    private final AtomicReference<String> currentScanningFile = new AtomicReference<>("");
    private final AtomicInteger newVideosCount = new AtomicInteger(0);
    private final AtomicInteger updatedVideosCount = new AtomicInteger(0);
    private final AtomicInteger skippedVideosCount = new AtomicInteger(0);
    
    @Override
    @Transactional
    public ScanResultResponse scanFolder(String folderPath, boolean recursive, boolean updateExisting) {
        if (isScanning.get()) {
            throw new IllegalStateException("扫描任务正在进行中");
        }
        
        Path rootPath = Paths.get(folderPath).toAbsolutePath().normalize();
        
        if (!Files.exists(rootPath)) {
            throw new IllegalArgumentException("文件夹不存在: " + folderPath);
        }
        
        if (!Files.isDirectory(rootPath)) {
            throw new IllegalArgumentException("不是有效的文件夹路径: " + folderPath);
        }
        
        isScanning.set(true);
        cancelRequested.set(false);
        scanProgress.set(0);
        currentScanningFile.set("");
        newVideosCount.set(0);
        updatedVideosCount.set(0);
        skippedVideosCount.set(0);
        
        AtomicInteger newCount = new AtomicInteger(0);
        AtomicInteger updatedCount = new AtomicInteger(0);
        AtomicInteger skippedCount = new AtomicInteger(0);
        List<Video> videosToSave = new ArrayList<>();
        
        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                    if (cancelRequested.get()) {
                        return FileVisitResult.TERMINATE;
                    }
                    
                    String fileName = path.getFileName().toString();
                    if (FileUtil.isVideoFile(fileName)) {
                        currentScanningFile.set(path.toString());
                        processVideoFile(path, fileName, updateExisting, newCount, updatedCount, skippedCount, videosToSave);
                        newVideosCount.set(newCount.get());
                        updatedVideosCount.set(updatedCount.get());
                        skippedVideosCount.set(skippedCount.get());
                    }
                    
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (cancelRequested.get()) {
                        return FileVisitResult.TERMINATE;
                    }
                    currentScanningFile.set(dir.toString());
                    return recursive ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
                }
            });
            
            if (!videosToSave.isEmpty()) {
                videoRepository.saveAll(videosToSave);
            }
            
            log.info("文件夹扫描完成: 新增={}, 更新={}, 跳过={}", newCount.get(), updatedCount.get(), skippedCount.get());
            
            return ScanResultResponse.builder()
                    .newVideos(newCount.get())
                    .updatedVideos(updatedCount.get())
                    .skippedVideos(skippedCount.get())
                    .totalVideos(videoRepository.findAll().size())
                    .build();
                    
        } catch (IOException e) {
            log.error("文件夹扫描失败", e);
            throw new RuntimeException("文件夹扫描失败: " + e.getMessage(), e);
        } finally {
            isScanning.set(false);
            scanProgress.set(100);
            currentScanningFile.set("");
        }
    }
    
    @Override
    public ScanResultResponse scanFolder(String folderPath) {
        return scanFolder(folderPath, true, false);
    }
    
    @Override
    public int getScanProgress() {
        return scanProgress.get();
    }
    
    @Override
    public boolean isScanning() {
        return isScanning.get();
    }
    
    @Override
    public void cancelScan() {
        cancelRequested.set(true);
        log.info("扫描任务已请求取消");
    }
    
    @Override
    public String getCurrentScanningFile() {
        return currentScanningFile.get();
    }
    
    @Override
    public int getNewVideosCount() {
        return newVideosCount.get();
    }
    
    @Override
    public int getUpdatedVideosCount() {
        return updatedVideosCount.get();
    }
    
    @Override
    public int getSkippedVideosCount() {
        return skippedVideosCount.get();
    }
    
    private void processVideoFile(Path path, String fileName, boolean updateExisting,
                                   AtomicInteger newCount, AtomicInteger updatedCount,
                                   AtomicInteger skippedCount, List<Video> videosToSave) {
        String absolutePath = path.toAbsolutePath().toString();
        
        try {
            Optional<Video> existingVideo = videoRepository.findByFilePath(absolutePath);
            
            if (existingVideo.isPresent()) {
                if (updateExisting) {
                    Video video = existingVideo.get();
                    try {
                        video.setFileSize(Files.size(path));
                    } catch (IOException e) {
                        log.warn("无法读取文件大小: {}", absolutePath, e);
                    }
                    
                    extractAndSetMetadata(video, path);
                    
                    videosToSave.add(video);
                    updatedCount.incrementAndGet();
                    log.debug("已更新视频: {}", absolutePath);
                } else {
                    skippedCount.incrementAndGet();
                    log.debug("视频已存在，跳过: {}", absolutePath);
                }
            } else {
                Video video = new Video();
                video.setTitle(FileUtil.getFileNameWithoutExtension(fileName));
                video.setFilePath(absolutePath);
                video.setFileName(fileName);
                video.setFormat(FileUtil.getFileExtension(fileName));
                video.setSourceType(Video.SourceType.SCANNED);
                
                try {
                    video.setFileSize(Files.size(path));
                } catch (IOException e) {
                    log.warn("无法读取文件大小: {}", absolutePath, e);
                }
                
                extractAndSetMetadata(video, path);
                
                videosToSave.add(video);
                newCount.incrementAndGet();
                log.debug("发现新视频: {}", absolutePath);
            }
        } catch (Exception e) {
            log.error("处理视频文件失败: {}", absolutePath, e);
            skippedCount.incrementAndGet();
        }
    }
    
    private void extractAndSetMetadata(Video video, Path path) {
        if (!videoMetadataService.isFFmpegAvailable()) {
            log.debug("FFmpeg 不可用，跳过元数据提取: {}", path);
            return;
        }
        
        String oldCoverPath = video.getCoverPath();
        
        try {
            VideoMetadata metadata = videoMetadataService.extractMetadata(path);
            
            if (metadata.isSuccess()) {
                if (metadata.getDuration() != null) {
                    video.setDuration(metadata.getDuration());
                    log.debug("提取到视频时长: {}秒", metadata.getDuration());
                }
                
                if (metadata.getResolution() != null) {
                    video.setResolution(metadata.getResolution());
                    log.debug("提取到视频分辨率: {}", metadata.getResolution());
                }
                
                if (metadata.getCoverPath() != null) {
                    video.setCoverPath(metadata.getCoverPath());
                    log.debug("提取到视频封面: {}", metadata.getCoverPath());
                    
                    if (oldCoverPath != null && !oldCoverPath.equals(metadata.getCoverPath())) {
                        videoMetadataService.deleteCover(oldCoverPath);
                        log.debug("已删除旧封面: {}", oldCoverPath);
                    }
                }
            } else {
                log.warn("元数据提取失败: {}", metadata.getErrorMessage());
            }
        } catch (Exception e) {
            log.warn("提取元数据时出错: {}", path, e);
        }
    }
}

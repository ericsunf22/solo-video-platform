package com.solo.video.service.impl;

import com.solo.video.entity.Video;
import com.solo.video.repository.VideoRepository;
import com.solo.video.service.CoverIntegrityService;
import com.solo.video.service.VideoMetadataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoverIntegrityServiceImpl implements CoverIntegrityService {
    
    private final VideoRepository videoRepository;
    private final VideoMetadataService videoMetadataService;
    
    @Value("${app.file.storage.cover-path:./storage/covers}")
    private String coverStoragePath;
    
    private Path coversPath;
    private final AtomicBoolean checkRunning = new AtomicBoolean(false);
    
    @PostConstruct
    public void init() {
        this.coversPath = Paths.get(coverStoragePath).toAbsolutePath().normalize();
        log.info("封面完整性校验服务初始化完成，封面路径: {}", coversPath);
    }
    
    @Override
    public IntegrityCheckResult checkIntegrity() {
        if (checkRunning.get()) {
            log.warn("完整性校验正在进行中");
            return null;
        }
        
        checkRunning.set(true);
        
        try {
            log.info("开始封面完整性校验");
            
            List<Video> allVideos = videoRepository.findAll();
            Set<String> coverPathsInDatabase = new HashSet<>();
            List<String> missingCovers = new ArrayList<>();
            int videosWithValidCover = 0;
            
            for (Video video : allVideos) {
                String coverPath = video.getCoverPath();
                if (coverPath != null && !coverPath.trim().isEmpty()) {
                    coverPathsInDatabase.add(coverPath);
                    Path coverFile = coversPath.resolve(coverPath);
                    if (Files.exists(coverFile)) {
                        videosWithValidCover++;
                    } else {
                        missingCovers.add(video.getTitle() + " (" + coverPath + ")");
                    }
                }
            }
            
            Set<String> coverFilesOnDisk = new HashSet<>();
            if (Files.exists(coversPath)) {
                try (Stream<Path> paths = Files.list(coversPath)) {
                    Iterator<Path> iterator = paths.iterator();
                    while (iterator.hasNext()) {
                        Path path = iterator.next();
                        if (Files.isRegularFile(path)) {
                            coverFilesOnDisk.add(path.getFileName().toString());
                        }
                    }
                }
            }
            
            List<String> orphanCovers = coverFilesOnDisk.stream()
                .filter(file -> !coverPathsInDatabase.contains(file))
                .collect(Collectors.toList());
            
            IntegrityCheckResult result = new IntegrityCheckResult(
                allVideos.size(),
                videosWithValidCover,
                missingCovers.size(),
                coverFilesOnDisk.size(),
                orphanCovers.size(),
                missingCovers,
                orphanCovers
            );
            
            log.info("完整性校验完成:");
            log.info("  总视频数: {}", result.totalVideos());
            log.info("  有有效封面的视频: {}", result.videosWithValidCover());
            log.info("  缺失封面的视频: {}", result.videosWithMissingCover());
            log.info("  磁盘上的封面文件: {}", result.totalCoverFiles());
            log.info("  孤儿封面文件: {}", result.orphanCoverFiles());
            
            if (!missingCovers.isEmpty()) {
                log.warn("缺失封面的视频: {}", missingCovers);
            }
            if (!orphanCovers.isEmpty()) {
                log.warn("孤儿封面文件 ({}/{}): {}", 
                    orphanCovers.size(), coverFilesOnDisk.size(), 
                    orphanCovers.size() > 20 ? orphanCovers.subList(0, 20) + "..." : orphanCovers);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("完整性校验失败", e);
            return null;
        } finally {
            checkRunning.set(false);
        }
    }
    
    @Override
    public int cleanOrphanCovers() {
        log.info("开始清理孤儿封面文件");
        
        IntegrityCheckResult checkResult = checkIntegrity();
        if (checkResult == null || checkResult.orphanCovers().isEmpty()) {
            log.info("没有需要清理的孤儿封面文件");
            return 0;
        }
        
        int deletedCount = 0;
        for (String orphanCover : checkResult.orphanCovers()) {
            try {
                Path coverFile = coversPath.resolve(orphanCover);
                if (Files.exists(coverFile)) {
                    Files.delete(coverFile);
                    deletedCount++;
                    log.debug("已删除孤儿封面: {}", coverFile);
                }
            } catch (IOException e) {
                log.warn("删除孤儿封面失败: {}", orphanCover, e);
            }
        }
        
        log.info("孤儿封面清理完成，共删除 {} 个文件", deletedCount);
        return deletedCount;
    }
    
    @Override
    @Transactional
    @Async
    public int repairMissingCovers(boolean forceRegenerate) {
        log.info("开始修复缺失封面 (forceRegenerate={})", forceRegenerate);
        
        List<Video> videosToRepair;
        
        if (forceRegenerate) {
            videosToRepair = videoRepository.findAll();
            log.info("强制重新生成所有 {} 个视频的封面", videosToRepair.size());
        } else {
            videosToRepair = videoRepository.findAll().stream()
                .filter(video -> video.getCoverPath() == null || video.getCoverPath().trim().isEmpty())
                .collect(Collectors.toList());
            log.info("需要修复封面的视频数: {}", videosToRepair.size());
        }
        
        if (!videoMetadataService.isFFmpegAvailable()) {
            log.error("FFmpeg 不可用，无法生成封面");
            return 0;
        }
        
        int repairedCount = 0;
        
        for (Video video : videosToRepair) {
            try {
                Path videoPath = Paths.get(video.getFilePath());
                
                if (!Files.exists(videoPath)) {
                    log.warn("视频文件不存在: {}", video.getFilePath());
                    continue;
                }
                
                String oldCoverPath = video.getCoverPath();
                
                com.solo.video.dto.VideoMetadata metadata = 
                    videoMetadataService.extractMetadata(videoPath, true);
                
                if (metadata.isSuccess()) {
                    if (metadata.getDuration() != null) {
                        video.setDuration(metadata.getDuration());
                    }
                    if (metadata.getResolution() != null) {
                        video.setResolution(metadata.getResolution());
                    }
                    if (metadata.getCoverPath() != null) {
                        video.setCoverPath(metadata.getCoverPath());
                        
                        if (oldCoverPath != null && !oldCoverPath.equals(metadata.getCoverPath())) {
                            videoMetadataService.deleteCover(oldCoverPath);
                            log.debug("已删除旧封面: {}", oldCoverPath);
                        }
                        
                        repairedCount++;
                        log.debug("已修复视频封面: {}", video.getTitle());
                    }
                    
                    videoRepository.save(video);
                }
                
            } catch (Exception e) {
                log.error("修复视频封面失败: {}", video.getTitle(), e);
            }
        }
        
        log.info("封面修复完成，共修复 {} 个视频", repairedCount);
        return repairedCount;
    }
    
    @Override
    public boolean isCheckRunning() {
        return checkRunning.get();
    }
}

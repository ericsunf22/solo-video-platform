package com.solo.video.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solo.video.config.FileStorageConfig;
import com.solo.video.dto.VideoMetadata;
import com.solo.video.service.FileStorageService;
import com.solo.video.service.VideoMetadataService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoMetadataServiceImpl implements VideoMetadataService {
    
    private final FileStorageConfig fileStorageConfig;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;
    
    private Path coverPath;
    private boolean ffprobeAvailable = false;
    private boolean ffmpegAvailable = false;
    
    private static final String[] FFPROBE_COMMANDS = {"ffprobe", "ffprobe.exe"};
    private static final String[] FFMPEG_COMMANDS = {"ffmpeg", "ffmpeg.exe"};
    
    @PostConstruct
    public void init() {
        coverPath = Paths.get(fileStorageConfig.getStorage().getCoverPath()).toAbsolutePath().normalize();
        
        ffprobeAvailable = checkCommandAvailable(FFPROBE_COMMANDS);
        ffmpegAvailable = checkCommandAvailable(FFMPEG_COMMANDS);
        
        log.info("FFprobe 检测结果: {}", ffprobeAvailable ? "可用" : "不可用");
        log.info("FFmpeg 检测结果: {}", ffmpegAvailable ? "可用" : "不可用");
    }
    
    private boolean checkCommandAvailable(String[] commands) {
        for (String cmd : commands) {
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd, "-version");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                
                boolean success = process.waitFor(10, TimeUnit.SECONDS);
                
                if (success && process.exitValue() == 0) {
                    return true;
                }
            } catch (Exception e) {
                log.debug("命令 {} 不可用: {}", cmd, e.getMessage());
            }
        }
        return false;
    }
    
    @Override
    public boolean isFFmpegAvailable() {
        return ffprobeAvailable && ffmpegAvailable;
    }
    
    @Override
    public VideoMetadata extractMetadata(Path videoPath) {
        return extractMetadata(videoPath, true);
    }
    
    @Override
    public VideoMetadata extractMetadata(Path videoPath, boolean extractCover) {
        if (!ffprobeAvailable) {
            return VideoMetadata.failure("FFprobe 不可用");
        }
        
        if (!Files.exists(videoPath)) {
            return VideoMetadata.failure("视频文件不存在: " + videoPath);
        }
        
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(getAvailableCommand(FFPROBE_COMMANDS));
            cmd.add("-v");
            cmd.add("quiet");
            cmd.add("-print_format");
            cmd.add("json");
            cmd.add("-show_format");
            cmd.add("-show_streams");
            cmd.add(videoPath.toString());
            
            log.debug("执行 FFprobe 命令: {}", String.join(" ", cmd));
            
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }
            
            boolean success = process.waitFor(30, TimeUnit.SECONDS);
            
            if (!success || process.exitValue() != 0) {
                log.warn("FFprobe 执行失败，退出码: {}", process.exitValue());
                return VideoMetadata.failure("FFprobe 执行失败");
            }
            
            String jsonOutput = output.toString();
            log.debug("FFprobe 输出: {}", jsonOutput.substring(0, Math.min(jsonOutput.length(), 500)));
            
            JsonNode root = objectMapper.readTree(jsonOutput);
            
            Long duration = extractDuration(root);
            String resolution = extractResolution(root);
            String coverPathStr = null;
            
            if (extractCover && ffmpegAvailable && duration != null && duration > 0) {
                coverPathStr = extractCover(videoPath, duration);
            }
            
            return VideoMetadata.success(duration, resolution, coverPathStr);
            
        } catch (Exception e) {
            log.error("提取视频元数据失败: {}", videoPath, e);
            return VideoMetadata.failure(e.getMessage());
        }
    }
    
    private String getAvailableCommand(String[] commands) {
        for (String cmd : commands) {
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd, "-version");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                boolean success = process.waitFor(10, TimeUnit.SECONDS);
                if (success && process.exitValue() == 0) {
                    return cmd;
                }
            } catch (Exception ignored) {
            }
        }
        return commands[0];
    }
    
    private Long extractDuration(JsonNode root) {
        try {
            JsonNode format = root.get("format");
            if (format != null && format.has("duration")) {
                String durationStr = format.get("duration").asText();
                double duration = Double.parseDouble(durationStr);
                return Math.round(duration);
            }
            
            JsonNode streams = root.get("streams");
            if (streams != null && streams.isArray()) {
                for (JsonNode stream : streams) {
                    String codecType = stream.has("codec_type") ? stream.get("codec_type").asText() : null;
                    if ("video".equals(codecType) && stream.has("duration")) {
                        String durationStr = stream.get("duration").asText();
                        double duration = Double.parseDouble(durationStr);
                        return Math.round(duration);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("提取视频时长失败: {}", e.getMessage());
        }
        return null;
    }
    
    private String extractResolution(JsonNode root) {
        try {
            JsonNode streams = root.get("streams");
            if (streams != null && streams.isArray()) {
                for (JsonNode stream : streams) {
                    String codecType = stream.has("codec_type") ? stream.get("codec_type").asText() : null;
                    if ("video".equals(codecType)) {
                        Integer width = stream.has("width") ? stream.get("width").asInt() : null;
                        Integer height = stream.has("height") ? stream.get("height").asInt() : null;
                        if (width != null && height != null) {
                            return width + "x" + height;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("提取视频分辨率失败: {}", e.getMessage());
        }
        return null;
    }
    
    private String extractCover(Path videoPath, long duration) {
        try {
            double extractTime = Math.min(duration * 0.1, 10.0);
            
            String coverFileName = UUID.randomUUID() + ".jpg";
            Path outputCoverPath = coverPath.resolve(coverFileName);
            
            if (!Files.exists(coverPath)) {
                Files.createDirectories(coverPath);
            }
            
            List<String> cmd = new ArrayList<>();
            cmd.add(getAvailableCommand(FFMPEG_COMMANDS));
            cmd.add("-ss");
            cmd.add(String.format("%.3f", extractTime));
            cmd.add("-i");
            cmd.add(videoPath.toString());
            cmd.add("-vframes");
            cmd.add("1");
            cmd.add("-q:v");
            cmd.add("2");
            cmd.add("-y");
            cmd.add(outputCoverPath.toString());
            
            log.debug("执行 FFmpeg 提取封面: {}", String.join(" ", cmd));
            
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            boolean success = process.waitFor(60, TimeUnit.SECONDS);
            
            if (!success || process.exitValue() != 0) {
                log.warn("FFmpeg 提取封面失败，退出码: {}", process.exitValue());
                return null;
            }
            
            if (Files.exists(outputCoverPath)) {
                log.info("封面图提取成功: {}", outputCoverPath);
                return coverFileName;
            }
            
        } catch (Exception e) {
            log.warn("提取视频封面失败: {}", videoPath, e);
        }
        return null;
    }
}

package com.solo.video.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solo.video.config.FileStorageConfig;
import com.solo.video.dto.VideoMetadata;
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
            
            if (extractCover && ffmpegAvailable) {
                long effectiveDuration = (duration != null && duration > 0) ? duration : 30;
                coverPathStr = extractCover(videoPath, effectiveDuration);
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
            if (format != null) {
                if (format.has("duration")) {
                    String durationStr = format.get("duration").asText();
                    try {
                        double duration = Double.parseDouble(durationStr);
                        log.debug("从 format.duration 提取到时长: {}秒", duration);
                        return Math.round(duration);
                    } catch (NumberFormatException e) {
                        log.debug("format.duration 解析失败: {}", durationStr);
                    }
                }
                
                if (format.has("tags")) {
                    JsonNode tags = format.get("tags");
                    if (tags.has("DURATION")) {
                        String durationStr = tags.get("DURATION").asText();
                        Long duration = parseDurationString(durationStr);
                        if (duration != null) {
                            log.debug("从 format.tags.DURATION 提取到时长: {}秒", duration);
                            return duration;
                        }
                    }
                }
            }
            
            JsonNode streams = root.get("streams");
            if (streams != null && streams.isArray()) {
                for (JsonNode stream : streams) {
                    if (stream.has("duration")) {
                        String durationStr = stream.get("duration").asText();
                        try {
                            double duration = Double.parseDouble(durationStr);
                            String codecType = stream.has("codec_type") ? stream.get("codec_type").asText() : "unknown";
                            log.debug("从 {} 流的 duration 提取到时长: {}秒", codecType, duration);
                            return Math.round(duration);
                        } catch (NumberFormatException e) {
                            log.debug("stream.duration 解析失败: {}", durationStr);
                        }
                    }
                    
                    if (stream.has("tags")) {
                        JsonNode tags = stream.get("tags");
                        if (tags.has("DURATION")) {
                            String durationStr = tags.get("DURATION").asText();
                            Long duration = parseDurationString(durationStr);
                            if (duration != null) {
                                String codecType = stream.has("codec_type") ? stream.get("codec_type").asText() : "unknown";
                                log.debug("从 {} 流的 tags.DURATION 提取到时长: {}秒", codecType, duration);
                                return duration;
                            }
                        }
                    }
                }
            }
            
            log.warn("无法从视频元数据中提取时长");
        } catch (Exception e) {
            log.warn("提取视频时长失败: {}", e.getMessage());
        }
        return null;
    }
    
    private Long parseDurationString(String durationStr) {
        try {
            if (durationStr == null || durationStr.trim().isEmpty()) {
                return null;
            }
            
            if (durationStr.matches("\\d{2}:\\d{2}:\\d{2}\\.\\d+")) {
                String[] parts = durationStr.split(":");
                if (parts.length == 3) {
                    int hours = Integer.parseInt(parts[0]);
                    int minutes = Integer.parseInt(parts[1]);
                    double seconds = Double.parseDouble(parts[2]);
                    return Math.round(hours * 3600 + minutes * 60 + seconds);
                }
            }
        } catch (Exception e) {
            log.debug("解析时长字符串失败: {}", durationStr);
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
            if (!Files.exists(coverPath)) {
                Files.createDirectories(coverPath);
            }
            
            List<Double> extractTimes = new ArrayList<>();
            
            if (duration > 0) {
                extractTimes.add(Math.min(duration * 0.05, 5.0));
                extractTimes.add(Math.min(duration * 0.1, 10.0));
                extractTimes.add(Math.min(duration * 0.2, 20.0));
                extractTimes.add(Math.min(duration * 0.5, 60.0));
            } else {
                extractTimes.add(1.0);
                extractTimes.add(3.0);
                extractTimes.add(5.0);
                extractTimes.add(10.0);
            }
            
            for (double extractTime : extractTimes) {
                log.debug("尝试在时间点 {} 秒提取封面", extractTime);
                
                String coverFileName = UUID.randomUUID() + ".jpg";
                Path outputCoverPath = coverPath.resolve(coverFileName);
                
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
                
                try {
                    ProcessBuilder pb = new ProcessBuilder(cmd);
                    pb.redirectErrorStream(true);
                    Process process = pb.start();
                    
                    boolean success = process.waitFor(30, TimeUnit.SECONDS);
                    
                    if (success && process.exitValue() == 0) {
                        if (Files.exists(outputCoverPath) && Files.size(outputCoverPath) > 1000) {
                            log.info("封面图提取成功 (时间点: {}秒): {}", extractTime, outputCoverPath);
                            return coverFileName;
                        } else {
                            log.debug("封面文件过小或不存在，尝试下一个时间点");
                            if (Files.exists(outputCoverPath)) {
                                Files.delete(outputCoverPath);
                            }
                        }
                    } else {
                        log.debug("FFmpeg 提取封面失败 (时间点: {}秒)，退出码: {}", extractTime, process.exitValue());
                    }
                } catch (Exception e) {
                    log.debug("提取封面失败 (时间点: {}秒): {}", extractTime, e.getMessage());
                }
            }
            
            log.warn("所有时间点提取封面均失败: {}", videoPath);
            
        } catch (Exception e) {
            log.warn("提取视频封面失败: {}", videoPath, e);
        }
        return null;
    }
    
    @Override
    public void deleteCover(String coverFileName) {
        if (coverFileName == null || coverFileName.trim().isEmpty()) {
            return;
        }
        
        try {
            Path coverFile = this.coverPath.resolve(coverFileName);
            if (Files.exists(coverFile)) {
                Files.delete(coverFile);
                log.info("已删除封面文件: {}", coverFile);
            }
        } catch (Exception e) {
            log.warn("删除封面文件失败: {}", coverFileName, e);
        }
    }
}

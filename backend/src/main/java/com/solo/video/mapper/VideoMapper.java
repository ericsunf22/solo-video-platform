package com.solo.video.mapper;

import com.solo.video.dto.response.TagResponse;
import com.solo.video.dto.response.VideoResponse;
import com.solo.video.entity.Tag;
import com.solo.video.entity.Video;
import com.solo.video.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class VideoMapper {
    
    private final TagRepository tagRepository;
    
    public VideoResponse toResponse(Video video) {
        if (video == null) {
            return null;
        }
        
        List<TagResponse> tags = video.getTags().stream()
                .map(this::toTagResponse)
                .collect(Collectors.toList());
        
        return VideoResponse.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .filePath(video.getFilePath())
                .fileName(video.getFileName())
                .fileSize(video.getFileSize())
                .duration(video.getDuration())
                .format(video.getFormat())
                .resolution(video.getResolution())
                .coverPath(video.getCoverPath())
                .sourceType(video.getSourceType())
                .isFavorite(video.getIsFavorite())
                .createdAt(video.getCreatedAt())
                .updatedAt(video.getUpdatedAt())
                .tags(tags)
                .formattedDuration(formatDuration(video.getDuration()))
                .formattedFileSize(formatFileSize(video.getFileSize()))
                .build();
    }
    
    public List<VideoResponse> toResponseList(List<Video> videos) {
        return videos.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    
    private TagResponse toTagResponse(Tag tag) {
        if (tag == null) {
            return null;
        }
        
        Long videoCount = tagRepository.countVideosByTagId(tag.getId());
        
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .color(tag.getColor())
                .description(tag.getDescription())
                .createdAt(tag.getCreatedAt())
                .updatedAt(tag.getUpdatedAt())
                .videoCount(videoCount)
                .build();
    }
    
    private String formatDuration(Long seconds) {
        if (seconds == null || seconds <= 0) {
            return "00:00";
        }
        
        long hrs = seconds / 3600;
        long mins = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        if (hrs > 0) {
            return String.format("%02d:%02d:%02d", hrs, mins, secs);
        }
        return String.format("%02d:%02d", mins, secs);
    }
    
    private String formatFileSize(Long bytes) {
        if (bytes == null || bytes <= 0) {
            return "0 B";
        }
        
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format(unitIndex > 0 ? "%.2f %s" : "%.0f %s", size, units[unitIndex]);
    }
}

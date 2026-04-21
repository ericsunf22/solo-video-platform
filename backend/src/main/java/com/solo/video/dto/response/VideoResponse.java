package com.solo.video.dto.response;

import com.solo.video.entity.Video;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoResponse {
    
    private Long id;
    private String title;
    private String description;
    private String filePath;
    private String fileName;
    private Long fileSize;
    private Long duration;
    private String format;
    private String resolution;
    private String coverPath;
    private Video.SourceType sourceType;
    private Boolean isFavorite;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TagResponse> tags;
    
    private String formattedDuration;
    private String formattedFileSize;
}

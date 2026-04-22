package com.solo.video.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VideoMetadata {
    
    private Long duration;
    
    private String resolution;
    
    private String coverPath;
    
    private boolean success;
    
    private String errorMessage;
    
    public static VideoMetadata success(Long duration, String resolution, String coverPath) {
        return VideoMetadata.builder()
                .duration(duration)
                .resolution(resolution)
                .coverPath(coverPath)
                .success(true)
                .build();
    }
    
    public static VideoMetadata failure(String errorMessage) {
        return VideoMetadata.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}

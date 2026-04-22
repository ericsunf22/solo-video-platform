package com.solo.video.service;

import com.solo.video.dto.VideoMetadata;

import java.nio.file.Path;

public interface VideoMetadataService {
    
    boolean isFFmpegAvailable();
    
    VideoMetadata extractMetadata(Path videoPath);
    
    VideoMetadata extractMetadata(Path videoPath, boolean extractCover);
}

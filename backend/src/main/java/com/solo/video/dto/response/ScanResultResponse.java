package com.solo.video.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanResultResponse {
    
    private int newVideos;
    private int updatedVideos;
    private int skippedVideos;
    private int totalVideos;
}

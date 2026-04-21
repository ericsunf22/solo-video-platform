package com.solo.video.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayHistoryResponse {
    
    private Long id;
    private Long videoId;
    private VideoResponse video;
    private Long progress;
    private Integer playCount;
    private Long totalPlayTime;
    private LocalDateTime lastPlayedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private String formattedProgress;
    private String formattedTotalPlayTime;
}

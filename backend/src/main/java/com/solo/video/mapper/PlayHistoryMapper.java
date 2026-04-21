package com.solo.video.mapper;

import com.solo.video.dto.response.PlayHistoryResponse;
import com.solo.video.entity.PlayHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlayHistoryMapper {
    
    private final VideoMapper videoMapper;
    
    public PlayHistoryResponse toResponse(PlayHistory history) {
        if (history == null) {
            return null;
        }
        
        return PlayHistoryResponse.builder()
                .id(history.getId())
                .videoId(history.getVideo().getId())
                .video(videoMapper.toResponse(history.getVideo()))
                .progress(history.getProgress())
                .playCount(history.getPlayCount())
                .totalPlayTime(history.getTotalPlayTime())
                .lastPlayedAt(history.getLastPlayedAt())
                .createdAt(history.getCreatedAt())
                .updatedAt(history.getUpdatedAt())
                .formattedProgress(formatDuration(history.getProgress()))
                .formattedTotalPlayTime(formatDuration(history.getTotalPlayTime()))
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
}

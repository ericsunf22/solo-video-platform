package com.solo.video.service;

import com.solo.video.dto.request.PlayProgressRequest;
import com.solo.video.dto.response.PlayHistoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PlayerService {
    
    void saveProgress(PlayProgressRequest request);
    
    PlayHistoryResponse getProgress(Long videoId);
    
    Page<PlayHistoryResponse> getPlayHistory(Pageable pageable);
    
    void clearPlayHistory();
    
    void incrementPlayCount(Long videoId);
}

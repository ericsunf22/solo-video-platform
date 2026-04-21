package com.solo.video.service.impl;

import com.solo.video.dto.request.PlayProgressRequest;
import com.solo.video.dto.response.PlayHistoryResponse;
import com.solo.video.entity.PlayHistory;
import com.solo.video.entity.Video;
import com.solo.video.mapper.PlayHistoryMapper;
import com.solo.video.repository.PlayHistoryRepository;
import com.solo.video.repository.VideoRepository;
import com.solo.video.service.PlayerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerServiceImpl implements PlayerService {
    
    private final PlayHistoryRepository playHistoryRepository;
    private final VideoRepository videoRepository;
    private final PlayHistoryMapper playHistoryMapper;
    
    @Override
    @Transactional
    public void saveProgress(PlayProgressRequest request) {
        Video video = videoRepository.findById(request.getVideoId())
                .orElseThrow(() -> new IllegalArgumentException("视频不存在: " + request.getVideoId()));
        
        PlayHistory playHistory = playHistoryRepository.findByVideoId(request.getVideoId())
                .orElseGet(() -> {
                    PlayHistory newHistory = new PlayHistory();
                    newHistory.setVideo(video);
                    return newHistory;
                });
        
        playHistory.setProgress(request.getProgress());
        playHistory.setLastPlayedAt(LocalDateTime.now());
        
        if (request.getDuration() != null && request.getDuration() > 0) {
            playHistory.setTotalPlayTime(playHistory.getTotalPlayTime() + request.getDuration());
        }
        
        playHistoryRepository.save(playHistory);
        log.debug("播放进度已保存: videoId={}, progress={}", request.getVideoId(), request.getProgress());
    }
    
    @Override
    public PlayHistoryResponse getProgress(Long videoId) {
        PlayHistory playHistory = playHistoryRepository.findByVideoId(videoId)
                .orElse(null);
        
        if (playHistory == null) {
            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new IllegalArgumentException("视频不存在: " + videoId));
            
            PlayHistory newHistory = new PlayHistory();
            newHistory.setVideo(video);
            newHistory.setProgress(0L);
            newHistory.setPlayCount(0);
            newHistory.setTotalPlayTime(0L);
            return playHistoryMapper.toResponse(newHistory);
        }
        
        return playHistoryMapper.toResponse(playHistory);
    }
    
    @Override
    public Page<PlayHistoryResponse> getPlayHistory(Pageable pageable) {
        Page<PlayHistory> historyPage = playHistoryRepository.findAllByOrderByLastPlayedAtDesc(pageable);
        return historyPage.map(playHistoryMapper::toResponse);
    }
    
    @Override
    @Transactional
    public void clearPlayHistory() {
        playHistoryRepository.deleteAll();
        log.info("播放历史已清空");
    }
    
    @Override
    @Transactional
    public void incrementPlayCount(Long videoId) {
        PlayHistory playHistory = playHistoryRepository.findByVideoId(videoId)
                .orElse(null);
        
        if (playHistory == null) {
            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new IllegalArgumentException("视频不存在: " + videoId));
            
            playHistory = new PlayHistory();
            playHistory.setVideo(video);
            playHistory.setPlayCount(1);
        } else {
            playHistory.setPlayCount(playHistory.getPlayCount() + 1);
        }
        
        playHistory.setLastPlayedAt(LocalDateTime.now());
        playHistoryRepository.save(playHistory);
        log.info("播放计数已增加: videoId={}", videoId);
    }
}

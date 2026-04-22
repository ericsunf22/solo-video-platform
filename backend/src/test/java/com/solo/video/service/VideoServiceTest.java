package com.solo.video.service;

import com.solo.video.dto.response.VideoResponse;
import com.solo.video.entity.Video;
import com.solo.video.exception.VideoNotFoundException;
import com.solo.video.mapper.VideoMapper;
import com.solo.video.repository.VideoRepository;
import com.solo.video.service.impl.VideoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoServiceTest {
    
    @Mock
    private VideoRepository videoRepository;
    
    @Mock
    private FileStorageService fileStorageService;
    
    @Mock
    private VideoMapper videoMapper;
    
    @InjectMocks
    private VideoServiceImpl videoService;
    
    private Video testVideo;
    private VideoResponse testVideoResponse;
    
    @BeforeEach
    void setUp() {
        testVideo = new Video();
        testVideo.setId(1L);
        testVideo.setTitle("测试视频");
        testVideo.setFilePath("/test/video.mp4");
        testVideo.setFileName("video.mp4");
        testVideo.setIsFavorite(false);
        testVideo.setSourceType(Video.SourceType.UPLOADED);
        
        testVideoResponse = VideoResponse.builder()
                .id(1L)
                .title("测试视频")
                .filePath("/test/video.mp4")
                .isFavorite(false)
                .build();
    }
    
    @Test
    void getVideoById_Success() {
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        when(videoMapper.toResponse(testVideo)).thenReturn(testVideoResponse);
        
        VideoResponse result = videoService.getVideoById(1L);
        
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("测试视频", result.getTitle());
    }
    
    @Test
    void getVideoById_NotFound() {
        when(videoRepository.findById(999L)).thenReturn(Optional.empty());
        
        assertThrows(VideoNotFoundException.class, () -> videoService.getVideoById(999L));
    }
    
    @SuppressWarnings({ "null", "unchecked" })
    @Test
    void getAllVideos_Success() {
        Page<Video> videoPage = new PageImpl<>(Collections.singletonList(testVideo));
        when(videoRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(videoPage);
        when(videoMapper.toResponse(testVideo)).thenReturn(testVideoResponse);
        
        Page<VideoResponse> result = videoService.getAllVideos(null, null, null, null, null, PageRequest.of(0, 10));
        
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
    
    @SuppressWarnings("null")
    @Test
    void toggleFavorite_FromFalseToTrue() {
        testVideo.setIsFavorite(false);
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        
        videoService.toggleFavorite(1L);
        
        assertTrue(testVideo.getIsFavorite());
        verify(videoRepository, times(1)).save(testVideo);
    }
    
    @SuppressWarnings("null")
    @Test
    void toggleFavorite_FromTrueToFalse() {
        testVideo.setIsFavorite(true);
        when(videoRepository.findById(1L)).thenReturn(Optional.of(testVideo));
        
        videoService.toggleFavorite(1L);
        
        assertFalse(testVideo.getIsFavorite());
        verify(videoRepository, times(1)).save(testVideo);
    }
    
    @Test
    void existsById_True() {
        when(videoRepository.existsById(1L)).thenReturn(true);
        
        boolean result = videoService.existsById(1L);
        
        assertTrue(result);
    }
    
    @Test
    void existsById_False() {
        when(videoRepository.existsById(999L)).thenReturn(false);
        
        boolean result = videoService.existsById(999L);
        
        assertFalse(result);
    }
    
    @Test
    void getVideoByFilePath_Found() {
        when(videoRepository.findByFilePath("/test/video.mp4")).thenReturn(Optional.of(testVideo));
        
        Optional<Video> result = videoService.getVideoByFilePath("/test/video.mp4");
        
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }
    
    @Test
    void getVideoByFilePath_NotFound() {
        when(videoRepository.findByFilePath("/nonexistent.mp4")).thenReturn(Optional.empty());
        
        Optional<Video> result = videoService.getVideoByFilePath("/nonexistent.mp4");
        
        assertFalse(result.isPresent());
    }
    
    @Test
    void countVideos() {
        when(videoRepository.count()).thenReturn(100L);
        
        long result = videoService.countVideos();
        
        assertEquals(100L, result);
    }
    
    @Test
    void countFavorites() {
        when(videoRepository.countFavorites()).thenReturn(25L);
        
        long result = videoService.countFavorites();
        
        assertEquals(25L, result);
    }
}

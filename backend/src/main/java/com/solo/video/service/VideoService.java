package com.solo.video.service;

import com.solo.video.dto.request.VideoUpdateRequest;
import com.solo.video.dto.response.VideoResponse;
import com.solo.video.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface VideoService {
    
    Page<VideoResponse> getAllVideos(String keyword, List<Long> tagIds, Boolean isFavorite, 
                                      String sortBy, String sortOrder, Pageable pageable);
    
    VideoResponse getVideoById(Long id);
    
    Video getVideoEntityById(Long id);
    
    Optional<Video> getVideoByFilePath(String filePath);
    
    VideoResponse uploadVideo(MultipartFile file, String title, String description);
    
    List<VideoResponse> uploadVideos(List<MultipartFile> files);
    
    VideoResponse updateVideo(Long id, VideoUpdateRequest request);
    
    void deleteVideo(Long id);
    
    void deleteVideos(List<Long> ids);
    
    long countVideos();
    
    long countFavorites();
    
    void toggleFavorite(Long videoId);
    
    void addToFavorites(List<Long> videoIds);
    
    void removeFromFavorites(List<Long> videoIds);
    
    boolean existsById(Long id);
}

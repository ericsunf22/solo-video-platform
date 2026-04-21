package com.solo.video.controller;

import com.solo.video.dto.response.ApiResponse;
import com.solo.video.dto.response.VideoResponse;
import com.solo.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {
    
    private final VideoService videoService;
    
    @PostMapping("/toggle/{videoId}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> toggleFavorite(@PathVariable Long videoId) {
        videoService.toggleFavorite(videoId);
        Map<String, Boolean> result = new HashMap<>();
        result.put("isFavorite", videoService.getVideoById(videoId).getIsFavorite());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<Page<VideoResponse>>> getFavorites(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder
    ) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<VideoResponse> favorites = videoService.getAllVideos(keyword, null, true, sortBy, sortOrder, pageable);
        return ResponseEntity.ok(ApiResponse.success(favorites));
    }
    
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<Void>> addToFavorites(@RequestBody List<Long> videoIds) {
        videoService.addToFavorites(videoIds);
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    @DeleteMapping("/batch")
    public ResponseEntity<ApiResponse<Void>> removeFromFavorites(@RequestBody List<Long> videoIds) {
        videoService.removeFromFavorites(videoIds);
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getFavoriteCount() {
        long count = videoService.countFavorites();
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}

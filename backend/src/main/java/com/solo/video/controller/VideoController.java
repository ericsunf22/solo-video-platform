package com.solo.video.controller;

import com.solo.video.dto.request.VideoUpdateRequest;
import com.solo.video.dto.response.ApiResponse;
import com.solo.video.dto.response.BatchUploadResult;
import com.solo.video.dto.response.VideoResponse;
import com.solo.video.service.VideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {
    
    private final VideoService videoService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<Page<VideoResponse>>> getAllVideos(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<Long> tagIds,
            @RequestParam(required = false) Boolean isFavorite,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder
    ) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<VideoResponse> videos = videoService.getAllVideos(keyword, tagIds, isFavorite, sortBy, sortOrder, pageable);
        return ResponseEntity.ok(ApiResponse.success(videos));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VideoResponse>> getVideoById(@PathVariable Long id) {
        VideoResponse video = videoService.getVideoById(id);
        return ResponseEntity.ok(ApiResponse.success(video));
    }
    
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<VideoResponse>> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description
    ) {
        VideoResponse video = videoService.uploadVideo(file, title, description);
        return ResponseEntity.ok(ApiResponse.success(video));
    }
    
    @PostMapping(value = "/upload/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BatchUploadResult>> uploadVideos(@RequestParam("files") List<MultipartFile> files) {
        BatchUploadResult result = videoService.uploadVideos(files);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VideoResponse>> updateVideo(
            @PathVariable Long id,
            @Valid @RequestBody VideoUpdateRequest request
    ) {
        VideoResponse video = videoService.updateVideo(id, request);
        return ResponseEntity.ok(ApiResponse.success(video));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVideo(@PathVariable Long id) {
        videoService.deleteVideo(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    @DeleteMapping("/batch")
    public ResponseEntity<ApiResponse<Void>> deleteVideos(@RequestBody List<Long> ids) {
        videoService.deleteVideos(ids);
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getVideoCount() {
        long count = videoService.countVideos();
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}

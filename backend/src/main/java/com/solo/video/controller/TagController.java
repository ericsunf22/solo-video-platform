package com.solo.video.controller;

import com.solo.video.dto.request.TagCreateRequest;
import com.solo.video.dto.response.ApiResponse;
import com.solo.video.dto.response.TagResponse;
import com.solo.video.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {
    
    private final TagService tagService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<TagResponse>>> getAllTags(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder
    ) {
        List<TagResponse> tags = tagService.getAllTags(keyword, sortBy, sortOrder);
        return ResponseEntity.ok(ApiResponse.success(tags));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TagResponse>> getTagById(@PathVariable Long id) {
        TagResponse tag = tagService.getTagById(id);
        return ResponseEntity.ok(ApiResponse.success(tag));
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<TagResponse>> createTag(@Valid @RequestBody TagCreateRequest request) {
        TagResponse tag = tagService.createTag(request);
        return ResponseEntity.ok(ApiResponse.success(tag));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TagResponse>> updateTag(
            @PathVariable Long id,
            @Valid @RequestBody TagCreateRequest request
    ) {
        TagResponse tag = tagService.updateTag(id, request);
        return ResponseEntity.ok(ApiResponse.success(tag));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTag(@PathVariable Long id) {
        tagService.deleteTag(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    @PostMapping("/video/{videoId}")
    public ResponseEntity<ApiResponse<Void>> addTagsToVideo(
            @PathVariable Long videoId,
            @RequestBody Map<String, List<Long>> request
    ) {
        List<Long> tagIds = request.get("tagIds");
        if (tagIds == null || tagIds.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("标签ID列表不能为空"));
        }
        tagService.addTagsToVideo(videoId, tagIds);
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    @DeleteMapping("/video/{videoId}/{tagId}")
    public ResponseEntity<ApiResponse<Void>> removeTagFromVideo(
            @PathVariable Long videoId,
            @PathVariable Long tagId
    ) {
        tagService.removeTagFromVideo(videoId, tagId);
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    @PostMapping("/videos/batch")
    public ResponseEntity<ApiResponse<Void>> addTagsToVideos(@RequestBody Map<String, List<Long>> request) {
        List<Long> videoIds = request.get("videoIds");
        List<Long> tagIds = request.get("tagIds");
        
        if (videoIds == null || videoIds.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("视频ID列表不能为空"));
        }
        if (tagIds == null || tagIds.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("标签ID列表不能为空"));
        }
        
        tagService.addTagsToVideos(videoIds, tagIds);
        return ResponseEntity.ok(ApiResponse.success());
    }
}

package com.solo.video.controller;

import com.solo.video.dto.request.PlayProgressRequest;
import com.solo.video.dto.response.ApiResponse;
import com.solo.video.dto.response.PlayHistoryResponse;
import com.solo.video.service.PlayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/player")
@RequiredArgsConstructor
public class PlayerController {
    
    private final PlayerService playerService;
    
    @PostMapping("/progress")
    public ResponseEntity<ApiResponse<Void>> saveProgress(@Valid @RequestBody PlayProgressRequest request) {
        playerService.saveProgress(request);
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    @GetMapping("/progress/{videoId}")
    public ResponseEntity<ApiResponse<PlayHistoryResponse>> getProgress(@PathVariable Long videoId) {
        PlayHistoryResponse progress = playerService.getProgress(videoId);
        return ResponseEntity.ok(ApiResponse.success(progress));
    }
    
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<PlayHistoryResponse>>> getPlayHistory(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<PlayHistoryResponse> history = playerService.getPlayHistory(pageable);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
    
    @DeleteMapping("/history")
    public ResponseEntity<ApiResponse<Void>> clearPlayHistory() {
        playerService.clearPlayHistory();
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    @PostMapping("/play/{videoId}")
    public ResponseEntity<ApiResponse<Void>> incrementPlayCount(@PathVariable Long videoId) {
        playerService.incrementPlayCount(videoId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}

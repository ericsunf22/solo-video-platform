package com.solo.video.controller;

import com.solo.video.dto.request.FolderScanRequest;
import com.solo.video.dto.response.ApiResponse;
import com.solo.video.dto.response.ScanResultResponse;
import com.solo.video.service.FileScanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/scan")
@RequiredArgsConstructor
public class ScanController {
    
    private final FileScanService fileScanService;
    
    @PostMapping("/folder")
    public ResponseEntity<ApiResponse<ScanResultResponse>> scanFolder(@Valid @RequestBody FolderScanRequest request) {
        ScanResultResponse result = fileScanService.scanFolder(
                request.getFolderPath(),
                Boolean.TRUE.equals(request.getRecursive()),
                Boolean.TRUE.equals(request.getUpdateExisting())
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    @GetMapping("/progress")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getScanProgress() {
        Map<String, Object> result = new HashMap<>();
        result.put("progress", fileScanService.getScanProgress());
        result.put("isScanning", fileScanService.isScanning());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelScan() {
        fileScanService.cancelScan();
        return ResponseEntity.ok(ApiResponse.success());
    }
}

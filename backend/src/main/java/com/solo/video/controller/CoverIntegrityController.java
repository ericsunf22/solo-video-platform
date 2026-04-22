package com.solo.video.controller;

import com.solo.video.dto.response.ApiResponse;
import com.solo.video.service.CoverIntegrityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/covers")
@RequiredArgsConstructor
public class CoverIntegrityController {
    
    private final CoverIntegrityService coverIntegrityService;
    
    @GetMapping("/integrity-check")
    public ApiResponse<CoverIntegrityService.IntegrityCheckResult> checkIntegrity() {
        log.info("执行封面完整性校验");
        CoverIntegrityService.IntegrityCheckResult result = coverIntegrityService.checkIntegrity();
        
        if (result == null) {
            return ApiResponse.error("完整性校验正在进行中");
        }
        
        return ApiResponse.success(result);
    }
    
    @PostMapping("/clean-orphans")
    public ApiResponse<Map<String, Object>> cleanOrphanCovers() {
        log.info("清理孤儿封面文件");
        
        int deletedCount = coverIntegrityService.cleanOrphanCovers();
        
        Map<String, Object> response = new HashMap<>();
        response.put("deletedCount", deletedCount);
        response.put("success", true);
        
        return ApiResponse.success(response, "清理完成，共删除 " + deletedCount + " 个孤儿文件");
    }
    
    @PostMapping("/repair")
    public ApiResponse<Map<String, Object>> repairMissingCovers(
            @RequestParam(defaultValue = "false") boolean forceRegenerate) {
        log.info("修复缺失封面 (forceRegenerate={})", forceRegenerate);
        
        int repairedCount = coverIntegrityService.repairMissingCovers(forceRegenerate);
        
        Map<String, Object> response = new HashMap<>();
        response.put("repairedCount", repairedCount);
        response.put("forceRegenerate", forceRegenerate);
        response.put("success", true);
        
        return ApiResponse.success(response, "修复完成，共修复 " + repairedCount + " 个视频");
    }
    
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("isCheckRunning", coverIntegrityService.isCheckRunning());
        return ApiResponse.success(response);
    }
}

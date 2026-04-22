package com.solo.video.controller;

import com.solo.video.dto.response.ApiResponse;
import com.solo.video.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
public class BackupController {
    
    private final BackupService backupService;
    
    @PostMapping("/create")
    public ApiResponse<Map<String, Object>> createBackup(
            @RequestParam(required = false) String name) {
        log.info("创建备份请求: name={}", name);
        
        BackupService.BackupResult result = backupService.createBackup(name);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.success());
        response.put("backupId", result.backupId());
        response.put("message", result.message());
        response.put("backupInfo", result.backupInfo());
        
        return result.success() 
            ? ApiResponse.success(response, "备份创建成功")
            : ApiResponse.error(result.message());
    }
    
    @GetMapping("/list")
    public ApiResponse<List<BackupService.BackupInfo>> listBackups() {
        log.info("获取备份列表");
        List<BackupService.BackupInfo> backups = backupService.listBackups();
        return ApiResponse.success(backups);
    }
    
    @GetMapping("/{backupId}")
    public ApiResponse<BackupService.BackupInfo> getBackup(@PathVariable String backupId) {
        log.info("获取备份信息: {}", backupId);
        return backupService.getBackupInfo(backupId)
            .map(ApiResponse::success)
            .orElse(ApiResponse.error("备份不存在: " + backupId));
    }
    
    @GetMapping("/validate/{backupId}")
    public ApiResponse<BackupService.RestoreValidationResult> validateBackup(@PathVariable String backupId) {
        log.info("验证备份: {}", backupId);
        BackupService.RestoreValidationResult result = backupService.validateBackup(backupId);
        return ApiResponse.success(result);
    }
    
    @PostMapping("/{backupId}/restore")
    public ApiResponse<Map<String, Object>> restoreBackup(@PathVariable String backupId) {
        log.info("恢复备份: {}", backupId);
        
        BackupService.RestoreResult result = backupService.restoreBackup(backupId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.success());
        response.put("backupId", backupId);
        response.put("message", result.message());
        response.put("requiresRestart", result.requiresRestart());
        
        return result.success() 
            ? ApiResponse.success(response, result.message())
            : ApiResponse.error(result.message());
    }
    
    @DeleteMapping("/{backupId}")
    public ApiResponse<Map<String, Object>> deleteBackup(@PathVariable String backupId) {
        log.info("删除备份: {}", backupId);
        
        boolean success = backupService.deleteBackup(backupId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("backupId", backupId);
        
        return success 
            ? ApiResponse.success(response, "备份删除成功")
            : ApiResponse.error("备份删除失败");
    }
    
    @PostMapping("/clean")
    public ApiResponse<Map<String, Object>> cleanOldBackups(
            @RequestParam(defaultValue = "7") int keepDays) {
        log.info("清理旧备份，保留 {} 天", keepDays);
        
        backupService.cleanOldBackups(keepDays);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("keepDays", keepDays);
        
        return ApiResponse.success(response, "旧备份清理完成");
    }
    
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getBackupStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("isRunning", backupService.isBackupRunning());
        response.put("databaseType", backupService.getDatabaseType().name());
        return ApiResponse.success(response);
    }
    
    @GetMapping("/database-type")
    public ApiResponse<Map<String, Object>> getDatabaseType() {
        BackupService.DatabaseType type = backupService.getDatabaseType();
        Map<String, Object> response = new HashMap<>();
        response.put("type", type.name());
        response.put("isFileBased", type == BackupService.DatabaseType.H2_FILE);
        response.put("isServerBased", type == BackupService.DatabaseType.MYSQL || 
                                      type == BackupService.DatabaseType.POSTGRESQL);
        return ApiResponse.success(response);
    }
}

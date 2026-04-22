package com.solo.video.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BackupService {
    
    BackupResult createBackup(String backupName);
    
    BackupResult createBackup();
    
    boolean restoreBackup(String backupId);
    
    boolean deleteBackup(String backupId);
    
    List<BackupInfo> listBackups();
    
    Optional<BackupInfo> getBackupInfo(String backupId);
    
    void cleanOldBackups(int keepDays);
    
    boolean isBackupRunning();
    
    record BackupInfo(
        String id,
        String name,
        LocalDateTime createdAt,
        long databaseSize,
        long coversSize,
        long totalSize,
        String description
    ) {}
    
    record BackupResult(
        boolean success,
        String backupId,
        String message,
        BackupInfo backupInfo
    ) {}
}

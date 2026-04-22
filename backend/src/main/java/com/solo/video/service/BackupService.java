package com.solo.video.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BackupService {
    
    BackupResult createBackup(String backupName);
    
    BackupResult createBackup();
    
    RestoreResult restoreBackup(String backupId);
    
    RestoreValidationResult validateBackup(String backupId);
    
    boolean deleteBackup(String backupId);
    
    List<BackupInfo> listBackups();
    
    Optional<BackupInfo> getBackupInfo(String backupId);
    
    void cleanOldBackups(int keepDays);
    
    boolean isBackupRunning();
    
    DatabaseType getDatabaseType();
    
    enum DatabaseType {
        H2_FILE,
        MYSQL,
        POSTGRESQL,
        UNKNOWN
    }
    
    record BackupInfo(
        String id,
        String name,
        LocalDateTime createdAt,
        long databaseSize,
        long coversSize,
        long totalSize,
        String description,
        DatabaseType databaseType,
        String appVersion,
        String checksum
    ) {}
    
    record BackupResult(
        boolean success,
        String backupId,
        String message,
        BackupInfo backupInfo
    ) {}
    
    record RestoreValidationResult(
        boolean valid,
        List<String> warnings,
        List<String> errors,
        BackupInfo backupInfo
    ) {}
    
    record RestoreResult(
        boolean success,
        String message,
        boolean requiresRestart
    ) {}
}

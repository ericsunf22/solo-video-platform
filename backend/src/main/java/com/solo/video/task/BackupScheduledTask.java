package com.solo.video.task;

import com.solo.video.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackupScheduledTask {
    
    private final BackupService backupService;
    
    @Value("${app.backup.scheduled.enabled:true}")
    private boolean scheduledEnabled;
    
    @Value("${app.backup.scheduled.cron:0 0 2 * * ?}")
    private String cronExpression;
    
    @Value("${app.backup.keep-days:7}")
    private int keepDays;
    
    @Scheduled(cron = "${app.backup.scheduled.cron:0 0 2 * * ?}")
    public void scheduledBackup() {
        if (!scheduledEnabled) {
            log.debug("定时备份已禁用");
            return;
        }
        
        log.info("开始执行定时备份任务");
        
        try {
            BackupService.BackupResult result = backupService.createBackup("自动备份-" + java.time.LocalDate.now());
            
            if (result.success()) {
                log.info("定时备份完成: {}", result.backupId());
                backupService.cleanOldBackups(keepDays);
            } else {
                log.warn("定时备份失败: {}", result.message());
            }
            
        } catch (Exception e) {
            log.error("定时备份任务执行失败", e);
        }
    }
}

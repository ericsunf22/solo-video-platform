package com.solo.video.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.solo.video.service.BackupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@Slf4j
@Service
public class BackupServiceImpl implements BackupService {
    
    @Value("${app.file.storage.path:./storage/videos}")
    private String videoStoragePath;
    
    @Value("${app.file.storage.cover-path:./storage/covers}")
    private String coverStoragePath;
    
    @Value("${spring.datasource.url:jdbc:h2:file:./data/video_db}")
    private String datasourceUrl;
    
    @Value("${app.backup.path:./storage/backups}")
    private String backupBasePath;
    
    @Value("${app.backup.keep-days:7}")
    private int defaultKeepDays;
    
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final AtomicBoolean backupRunning = new AtomicBoolean(false);
    private Path backupPath;
    private Path databasePath;
    private Path coversPath;
    
    @PostConstruct
    public void init() {
        this.backupPath = Paths.get(backupBasePath).toAbsolutePath().normalize();
        this.databasePath = extractDatabasePath(datasourceUrl);
        this.coversPath = Paths.get(coverStoragePath).toAbsolutePath().normalize();
        
        try {
            if (!Files.exists(backupPath)) {
                Files.createDirectories(backupPath);
                log.info("已创建备份目录: {}", backupPath);
            }
        } catch (IOException e) {
            log.error("创建备份目录失败: {}", backupPath, e);
        }
        
        log.info("备份服务初始化完成");
        log.info("  备份目录: {}", backupPath);
        log.info("  数据库路径: {}", databasePath);
        log.info("  封面路径: {}", coversPath);
    }
    
    private Path extractDatabasePath(String jdbcUrl) {
        String prefix = "jdbc:h2:file:";
        if (jdbcUrl.startsWith(prefix)) {
            String path = jdbcUrl.substring(prefix.length());
            int semicolonIndex = path.indexOf(';');
            if (semicolonIndex > 0) {
                path = path.substring(0, semicolonIndex);
            }
            return Paths.get(path).toAbsolutePath().normalize().getParent();
        }
        return Paths.get("./data").toAbsolutePath().normalize();
    }
    
    @Override
    @Async
    public BackupResult createBackup(String backupName) {
        if (backupRunning.get()) {
            return new BackupResult(false, null, "备份任务正在进行中", null);
        }
        
        backupRunning.set(true);
        String backupId = generateBackupId();
        String actualName = backupName != null && !backupName.trim().isEmpty() 
            ? backupName 
            : "backup-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        
        try {
            Path backupDir = backupPath.resolve(backupId);
            Files.createDirectories(backupDir);
            
            log.info("开始创建备份: {} (ID: {})", actualName, backupId);
            
            long databaseSize = backupDatabase(backupDir);
            long coversSize = backupCovers(backupDir);
            
            BackupInfo backupInfo = new BackupInfo(
                backupId,
                actualName,
                LocalDateTime.now(),
                databaseSize,
                coversSize,
                databaseSize + coversSize,
                "手动备份"
            );
            
            saveBackupInfo(backupDir, backupInfo);
            
            log.info("备份创建成功: {} (数据库: {} bytes, 封面: {} bytes, 总计: {} bytes)", 
                backupId, databaseSize, coversSize, databaseSize + coversSize);
            
            cleanOldBackups(defaultKeepDays);
            
            return new BackupResult(true, backupId, "备份创建成功", backupInfo);
            
        } catch (Exception e) {
            log.error("创建备份失败: {}", backupId, e);
            return new BackupResult(false, backupId, "备份创建失败: " + e.getMessage(), null);
        } finally {
            backupRunning.set(false);
        }
    }
    
    @Override
    public BackupResult createBackup() {
        return createBackup(null);
    }
    
    private long backupDatabase(Path backupDir) throws IOException {
        Path dbBackupDir = backupDir.resolve("database");
        Files.createDirectories(dbBackupDir);
        
        long totalSize = 0;
        
        if (databasePath != null && Files.exists(databasePath)) {
            try (Stream<Path> paths = Files.list(databasePath)) {
                Iterator<Path> iterator = paths.iterator();
                while (iterator.hasNext()) {
                    Path source = iterator.next();
                    Path target = dbBackupDir.resolve(source.getFileName());
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    totalSize += Files.size(target);
                    log.debug("已备份数据库文件: {}", source.getFileName());
                }
            }
        }
        
        return totalSize;
    }
    
    private long backupCovers(Path backupDir) throws IOException {
        if (coversPath == null || !Files.exists(coversPath)) {
            return 0;
        }
        
        Path coversBackupDir = backupDir.resolve("covers");
        Files.createDirectories(coversBackupDir);
        
        long totalSize = 0;
        
        try (Stream<Path> paths = Files.list(coversPath)) {
            Iterator<Path> iterator = paths.iterator();
            while (iterator.hasNext()) {
                Path source = iterator.next();
                if (Files.isRegularFile(source)) {
                    Path target = coversBackupDir.resolve(source.getFileName());
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    totalSize += Files.size(target);
                }
            }
        }
        
        log.debug("已备份封面文件，共 {} bytes", totalSize);
        return totalSize;
    }
    
    private void saveBackupInfo(Path backupDir, BackupInfo info) throws IOException {
        Path infoFile = backupDir.resolve("backup-info.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(infoFile.toFile(), info);
    }
    
    private BackupInfo loadBackupInfo(Path backupDir) {
        try {
            Path infoFile = backupDir.resolve("backup-info.json");
            if (Files.exists(infoFile)) {
                return objectMapper.readValue(infoFile.toFile(), BackupInfo.class);
            }
        } catch (IOException e) {
            log.warn("加载备份信息失败: {}", backupDir, e);
        }
        return null;
    }
    
    @Override
    public boolean restoreBackup(String backupId) {
        if (backupRunning.get()) {
            log.warn("备份任务正在进行中，无法恢复备份");
            return false;
        }
        
        Path backupDir = backupPath.resolve(backupId);
        
        if (!Files.exists(backupDir)) {
            log.error("备份不存在: {}", backupId);
            return false;
        }
        
        backupRunning.set(true);
        
        try {
            log.info("开始恢复备份: {}", backupId);
            
            restoreDatabase(backupDir);
            restoreCovers(backupDir);
            
            log.info("备份恢复成功: {}", backupId);
            return true;
            
        } catch (Exception e) {
            log.error("恢复备份失败: {}", backupId, e);
            return false;
        } finally {
            backupRunning.set(false);
        }
    }
    
    private void restoreDatabase(Path backupDir) throws IOException {
        Path dbBackupDir = backupDir.resolve("database");
        
        if (!Files.exists(dbBackupDir)) {
            log.warn("备份中没有数据库文件: {}", backupDir);
            return;
        }
        
        if (databasePath != null) {
            if (!Files.exists(databasePath)) {
                Files.createDirectories(databasePath);
            }
            
            try (Stream<Path> paths = Files.list(dbBackupDir)) {
                Iterator<Path> iterator = paths.iterator();
                while (iterator.hasNext()) {
                    Path source = iterator.next();
                    Path target = databasePath.resolve(source.getFileName());
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    log.debug("已恢复数据库文件: {}", source.getFileName());
                }
            }
        }
    }
    
    private void restoreCovers(Path backupDir) throws IOException {
        Path coversBackupDir = backupDir.resolve("covers");
        
        if (!Files.exists(coversBackupDir)) {
            log.warn("备份中没有封面文件: {}", backupDir);
            return;
        }
        
        if (coversPath != null) {
            if (!Files.exists(coversPath)) {
                Files.createDirectories(coversPath);
            } else {
                try (Stream<Path> paths = Files.list(coversPath)) {
                    paths.filter(Files::isRegularFile).forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("删除旧封面文件失败: {}", path);
                        }
                    });
                }
            }
            
            try (Stream<Path> paths = Files.list(coversBackupDir)) {
                Iterator<Path> iterator = paths.iterator();
                while (iterator.hasNext()) {
                    Path source = iterator.next();
                    if (Files.isRegularFile(source)) {
                        Path target = coversPath.resolve(source.getFileName());
                        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }
    
    @Override
    public boolean deleteBackup(String backupId) {
        Path backupDir = backupPath.resolve(backupId);
        
        if (!Files.exists(backupDir)) {
            log.warn("备份不存在: {}", backupId);
            return false;
        }
        
        try {
            deleteDirectory(backupDir);
            log.info("已删除备份: {}", backupId);
            return true;
        } catch (IOException e) {
            log.error("删除备份失败: {}", backupId, e);
            return false;
        }
    }
    
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (Stream<Path> paths = Files.walk(directory)) {
                paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("删除文件失败: {}", path, e);
                        }
                    });
            }
        }
    }
    
    @Override
    public List<BackupInfo> listBackups() {
        List<BackupInfo> backups = new ArrayList<>();
        
        if (!Files.exists(backupPath)) {
            return backups;
        }
        
        try (Stream<Path> paths = Files.list(backupPath)) {
            paths.filter(Files::isDirectory)
                .sorted(Comparator.comparing((Path p) -> {
                    BackupInfo info = loadBackupInfo(p);
                    return info != null ? info.createdAt() : LocalDateTime.MIN;
                }).reversed())
                .forEach(dir -> {
                    BackupInfo info = loadBackupInfo(dir);
                    if (info != null) {
                        backups.add(info);
                    }
                });
        } catch (IOException e) {
            log.error("列出备份失败", e);
        }
        
        return backups;
    }
    
    @Override
    public Optional<BackupInfo> getBackupInfo(String backupId) {
        Path backupDir = backupPath.resolve(backupId);
        
        if (!Files.exists(backupDir)) {
            return Optional.empty();
        }
        
        BackupInfo info = loadBackupInfo(backupDir);
        return Optional.ofNullable(info);
    }
    
    @Override
    public void cleanOldBackups(int keepDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(keepDays);
        
        log.info("开始清理 {} 天前的旧备份", keepDays);
        
        List<BackupInfo> backups = listBackups();
        int deletedCount = 0;
        
        for (BackupInfo backup : backups) {
            if (backup.createdAt().isBefore(cutoff)) {
                if (deleteBackup(backup.id())) {
                    deletedCount++;
                }
            }
        }
        
        log.info("清理完成，共删除 {} 个旧备份", deletedCount);
    }
    
    @Override
    public boolean isBackupRunning() {
        return backupRunning.get();
    }
    
    private String generateBackupId() {
        return UUID.randomUUID().toString();
    }
}

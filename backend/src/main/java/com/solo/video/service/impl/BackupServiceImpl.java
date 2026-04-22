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
import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    
    @Value("${spring.datasource.username:}")
    private String datasourceUsername;
    
    @Value("${spring.datasource.password:}")
    private String datasourcePassword;
    
    @Value("${app.backup.path:./storage/backups}")
    private String backupBasePath;
    
    @Value("${app.backup.keep-days:7}")
    private int defaultKeepDays;
    
    @Value("${app.backup.mysqldump-path:mysqldump}")
    private String mysqldumpPath;
    
    @Value("${app.backup.pgdump-path:pg_dump}")
    private String pgdumpPath;
    
    @Value("${info.app.version:1.0.0}")
    private String appVersion;
    
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final AtomicBoolean backupRunning = new AtomicBoolean(false);
    private Path backupPath;
    private Path databasePath;
    private Path coversPath;
    private DatabaseType databaseType;
    
    @PostConstruct
    public void init() {
        this.backupPath = Paths.get(backupBasePath).toAbsolutePath().normalize();
        this.databaseType = detectDatabaseType(datasourceUrl);
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
        log.info("  数据库类型: {}", databaseType);
        log.info("  数据库路径: {}", databasePath);
        log.info("  封面路径: {}", coversPath);
        log.info("  应用版本: {}", appVersion);
    }
    
    @Override
    public DatabaseType getDatabaseType() {
        return databaseType;
    }
    
    private DatabaseType detectDatabaseType(String jdbcUrl) {
        if (jdbcUrl == null) {
            return DatabaseType.UNKNOWN;
        }
        
        String lowerUrl = jdbcUrl.toLowerCase();
        
        if (lowerUrl.startsWith("jdbc:h2:file:")) {
            return DatabaseType.H2_FILE;
        } else if (lowerUrl.startsWith("jdbc:h2:")) {
            return DatabaseType.H2_FILE;
        } else if (lowerUrl.startsWith("jdbc:mysql:")) {
            return DatabaseType.MYSQL;
        } else if (lowerUrl.startsWith("jdbc:mariadb:")) {
            return DatabaseType.MYSQL;
        } else if (lowerUrl.startsWith("jdbc:postgresql:")) {
            return DatabaseType.POSTGRESQL;
        }
        
        log.warn("未知的数据库类型: {}", jdbcUrl);
        return DatabaseType.UNKNOWN;
    }
    
    private Path extractDatabasePath(String jdbcUrl) {
        if (databaseType == DatabaseType.H2_FILE) {
            String prefix = "jdbc:h2:file:";
            if (jdbcUrl.startsWith(prefix)) {
                String path = jdbcUrl.substring(prefix.length());
                int semicolonIndex = path.indexOf(';');
                if (semicolonIndex > 0) {
                    path = path.substring(0, semicolonIndex);
                }
                return Paths.get(path).toAbsolutePath().normalize().getParent();
            }
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
            log.info("数据库类型: {}", databaseType);
            
            long databaseSize;
            
            if (databaseType == DatabaseType.H2_FILE) {
                databaseSize = backupH2Database(backupDir);
            } else if (databaseType == DatabaseType.MYSQL) {
                databaseSize = backupMySQLDatabase(backupDir);
            } else if (databaseType == DatabaseType.POSTGRESQL) {
                databaseSize = backupPostgreSQLDatabase(backupDir);
            } else {
                log.warn("不支持的数据库类型: {}，跳过数据库备份", databaseType);
                databaseSize = 0;
            }
            
            long coversSize = backupCovers(backupDir);
            
            String checksum = calculateBackupChecksum(backupDir);
            
            BackupInfo backupInfo = new BackupInfo(
                backupId,
                actualName,
                LocalDateTime.now(),
                databaseSize,
                coversSize,
                databaseSize + coversSize,
                "手动备份",
                databaseType,
                appVersion,
                checksum
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
    
    private long backupH2Database(Path backupDir) throws IOException {
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
    
    private long backupMySQLDatabase(Path backupDir) throws IOException {
        Path dbBackupDir = backupDir.resolve("database");
        Files.createDirectories(dbBackupDir);
        
        Path dumpFile = dbBackupDir.resolve("dump.sql");
        
        log.info("尝试使用 mysqldump 备份 MySQL 数据库");
        
        String databaseName = extractDatabaseNameFromUrl(datasourceUrl);
        
        List<String> cmd = new ArrayList<>();
        cmd.add(mysqldumpPath);
        cmd.add("--user=" + datasourceUsername);
        if (datasourcePassword != null && !datasourcePassword.isEmpty()) {
            cmd.add("--password=" + datasourcePassword);
        }
        cmd.add("--databases");
        cmd.add(databaseName);
        
        log.debug("执行命令: {}", String.join(" ", cmd));
        
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectOutput(dumpFile.toFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            boolean success = process.waitFor(300, java.util.concurrent.TimeUnit.SECONDS);
            
            if (success && process.exitValue() == 0) {
                long size = Files.size(dumpFile);
                log.info("MySQL 数据库备份成功: {} ({} bytes)", dumpFile, size);
                return size;
            } else {
                log.warn("mysqldump 执行失败，退出码: {}", process.exitValue());
                log.warn("MySQL 备份失败，仅备份元数据");
                
                Path infoFile = dbBackupDir.resolve("backup-info.json");
                Map<String, Object> info = new HashMap<>();
                info.put("databaseType", "MYSQL");
                info.put("url", datasourceUrl);
                info.put("username", datasourceUsername);
                info.put("error", "mysqldump 执行失败，未备份数据");
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(infoFile.toFile(), info);
                
                return 0;
            }
        } catch (Exception e) {
            log.error("MySQL 数据库备份失败: {}", e.getMessage());
            return 0;
        }
    }
    
    private long backupPostgreSQLDatabase(Path backupDir) throws IOException {
        Path dbBackupDir = backupDir.resolve("database");
        Files.createDirectories(dbBackupDir);
        
        Path dumpFile = dbBackupDir.resolve("dump.sql");
        
        log.info("尝试使用 pg_dump 备份 PostgreSQL 数据库");
        
        String databaseName = extractDatabaseNameFromUrl(datasourceUrl);
        
        List<String> cmd = new ArrayList<>();
        cmd.add(pgdumpPath);
        cmd.add("--username=" + datasourceUsername);
        cmd.add("--dbname=" + databaseName);
        cmd.add("--file=" + dumpFile.toAbsolutePath());
        
        Map<String, String> env = new HashMap<>();
        if (datasourcePassword != null && !datasourcePassword.isEmpty()) {
            env.put("PGPASSWORD", datasourcePassword);
        }
        
        log.debug("执行命令: {}", String.join(" ", cmd));
        
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.environment().putAll(env);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            boolean success = process.waitFor(300, java.util.concurrent.TimeUnit.SECONDS);
            
            if (success && process.exitValue() == 0 && Files.exists(dumpFile)) {
                long size = Files.size(dumpFile);
                log.info("PostgreSQL 数据库备份成功: {} ({} bytes)", dumpFile, size);
                return size;
            } else {
                log.warn("pg_dump 执行失败，退出码: {}", process.exitValue());
                return 0;
            }
        } catch (Exception e) {
            log.error("PostgreSQL 数据库备份失败: {}", e.getMessage());
            return 0;
        }
    }
    
    private String extractDatabaseNameFromUrl(String jdbcUrl) {
        if (jdbcUrl == null) {
            return "";
        }
        
        try {
            int lastSlashIndex = jdbcUrl.lastIndexOf('/');
            if (lastSlashIndex > 0) {
                String path = jdbcUrl.substring(lastSlashIndex + 1);
                int queryIndex = path.indexOf('?');
                if (queryIndex > 0) {
                    path = path.substring(0, queryIndex);
                }
                int semicolonIndex = path.indexOf(';');
                if (semicolonIndex > 0) {
                    path = path.substring(0, semicolonIndex);
                }
                return path;
            }
        } catch (Exception e) {
            log.warn("解析数据库名称失败: {}", jdbcUrl);
        }
        
        return "video_db";
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
    
    private String calculateBackupChecksum(Path backupDir) {
        try {
            StringBuilder combined = new StringBuilder();
            
            try (Stream<Path> paths = Files.walk(backupDir)) {
                Iterator<Path> iterator = paths.iterator();
                while (iterator.hasNext()) {
                    Path path = iterator.next();
                    if (Files.isRegularFile(path) && !path.getFileName().toString().equals("backup-info.json")) {
                        String fileChecksum = calculateFileChecksum(path);
                        combined.append(fileChecksum);
                    }
                }
            }
            
            return calculateStringChecksum(combined.toString());
        } catch (Exception e) {
            log.warn("计算备份校验和失败: {}", e.getMessage());
            return null;
        }
    }
    
    private String calculateFileChecksum(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) > 0) {
                md.update(buffer, 0, read);
            }
            byte[] hash = md.digest();
            return bytesToHex(hash);
        } catch (Exception e) {
            log.debug("计算文件校验和失败: {}", path);
            return "";
        }
    }
    
    private String calculateStringChecksum(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
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
    public RestoreValidationResult validateBackup(String backupId) {
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        Path backupDir = backupPath.resolve(backupId);
        
        if (!Files.exists(backupDir)) {
            errors.add("备份不存在: " + backupId);
            return new RestoreValidationResult(false, warnings, errors, null);
        }
        
        BackupInfo backupInfo = loadBackupInfo(backupDir);
        
        if (backupInfo == null) {
            errors.add("无法加载备份信息");
            return new RestoreValidationResult(false, warnings, errors, null);
        }
        
        if (backupInfo.databaseType() != databaseType) {
            warnings.add("数据库类型不匹配: 备份=" + backupInfo.databaseType() + ", 当前=" + databaseType);
        }
        
        if (backupInfo.appVersion() != null && !backupInfo.appVersion().equals(appVersion)) {
            warnings.add("应用版本不匹配: 备份=" + backupInfo.appVersion() + ", 当前=" + appVersion);
        }
        
        Path dbBackupDir = backupDir.resolve("database");
        if (!Files.exists(dbBackupDir)) {
            errors.add("备份中没有数据库文件");
        } else {
            try (Stream<Path> paths = Files.list(dbBackupDir)) {
                if (paths.findAny().isEmpty()) {
                    errors.add("数据库备份目录为空");
                }
            } catch (IOException e) {
                errors.add("无法读取数据库备份目录: " + e.getMessage());
            }
        }
        
        if (backupInfo.checksum() != null) {
            String currentChecksum = calculateBackupChecksum(backupDir);
            if (currentChecksum != null && !currentChecksum.equals(backupInfo.checksum())) {
                warnings.add("备份校验和不匹配，备份文件可能已被修改");
            }
        }
        
        if (databaseType != DatabaseType.H2_FILE && databaseType != DatabaseType.UNKNOWN) {
            warnings.add("当前数据库类型为 " + databaseType + "，自动恢复可能需要手动导入 SQL 文件");
        }
        
        boolean valid = errors.isEmpty();
        
        return new RestoreValidationResult(valid, warnings, errors, backupInfo);
    }
    
    @Override
    public RestoreResult restoreBackup(String backupId) {
        if (backupRunning.get()) {
            return new RestoreResult(false, "备份任务正在进行中，无法恢复备份", false);
        }
        
        RestoreValidationResult validation = validateBackup(backupId);
        
        if (!validation.valid()) {
            return new RestoreResult(false, "备份验证失败: " + String.join(", ", validation.errors()), false);
        }
        
        if (!validation.warnings().isEmpty()) {
            log.warn("备份恢复存在警告: {}", validation.warnings());
        }
        
        backupRunning.set(true);
        Path backupDir = backupPath.resolve(backupId);
        
        try {
            log.info("开始恢复备份: {}", backupId);
            
            boolean requiresRestart = false;
            
            if (databaseType == DatabaseType.H2_FILE) {
                requiresRestart = restoreH2Database(backupDir);
            } else if (databaseType == DatabaseType.MYSQL) {
                restoreMySQLDatabase(backupDir);
            } else if (databaseType == DatabaseType.POSTGRESQL) {
                restorePostgreSQLDatabase(backupDir);
            } else {
                log.warn("不支持的数据库类型: {}，跳过数据库恢复", databaseType);
            }
            
            restoreCovers(backupDir);
            
            if (requiresRestart) {
                log.info("H2 数据库已恢复，建议重启应用");
            }
            
            log.info("备份恢复成功: {}", backupId);
            
            return new RestoreResult(
                true, 
                "备份恢复成功" + (requiresRestart ? "，建议重启应用" : ""), 
                requiresRestart
            );
            
        } catch (Exception e) {
            log.error("恢复备份失败: {}", backupId, e);
            return new RestoreResult(false, "恢复备份失败: " + e.getMessage(), false);
        } finally {
            backupRunning.set(false);
        }
    }
    
    private boolean restoreH2Database(Path backupDir) throws IOException {
        Path dbBackupDir = backupDir.resolve("database");
        
        if (!Files.exists(dbBackupDir)) {
            log.warn("备份中没有数据库文件: {}", backupDir);
            return false;
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
            
            return true;
        }
        
        return false;
    }
    
    private void restoreMySQLDatabase(Path backupDir) throws IOException {
        Path dbBackupDir = backupDir.resolve("database");
        Path dumpFile = dbBackupDir.resolve("dump.sql");
        
        if (!Files.exists(dumpFile)) {
            log.warn("备份中没有 MySQL 转储文件");
            return;
        }
        
        log.info("MySQL 数据库恢复需要手动执行:");
        log.info("  转储文件位置: {}", dumpFile.toAbsolutePath());
        log.info("  执行命令: mysql -u{} -p {} < {}", 
            datasourceUsername, 
            extractDatabaseNameFromUrl(datasourceUrl),
            dumpFile.toAbsolutePath());
        
        throw new IOException("MySQL 数据库恢复需要手动执行 SQL 文件: " + dumpFile.toAbsolutePath());
    }
    
    private void restorePostgreSQLDatabase(Path backupDir) throws IOException {
        Path dbBackupDir = backupDir.resolve("database");
        Path dumpFile = dbBackupDir.resolve("dump.sql");
        
        if (!Files.exists(dumpFile)) {
            log.warn("备份中没有 PostgreSQL 转储文件");
            return;
        }
        
        log.info("PostgreSQL 数据库恢复需要手动执行:");
        log.info("  转储文件位置: {}", dumpFile.toAbsolutePath());
        log.info("  执行命令: psql -U {} -d {} -f {}", 
            datasourceUsername, 
            extractDatabaseNameFromUrl(datasourceUrl),
            dumpFile.toAbsolutePath());
        
        throw new IOException("PostgreSQL 数据库恢复需要手动执行 SQL 文件: " + dumpFile.toAbsolutePath());
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

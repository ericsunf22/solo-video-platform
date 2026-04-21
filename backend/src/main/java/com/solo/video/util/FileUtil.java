package com.solo.video.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class FileUtil {
    
    private static final List<String> VIDEO_EXTENSIONS = Arrays.asList(
            "mp4", "avi", "mkv", "mov", "flv", "wmv", "webm", "m4v"
    );
    
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return fileName.substring(lastDot + 1).toLowerCase();
    }
    
    public static boolean isVideoFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }
        String extension = getFileExtension(file.getName());
        return VIDEO_EXTENSIONS.contains(extension);
    }
    
    public static boolean isVideoFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        String extension = getFileExtension(fileName);
        return VIDEO_EXTENSIONS.contains(extension);
    }
    
    public static String getFileNameWithoutExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) {
            return fileName;
        }
        return fileName.substring(0, lastDot);
    }
    
    public static String formatFileSize(long bytes) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format(unitIndex > 0 ? "%.2f %s" : "%.0f %s", size, units[unitIndex]);
    }
}

package com.solo.video.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {
    
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");
    
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }
    
    public static String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
    
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DATE_TIME_FORMATTER);
    }
    
    public static String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return null;
        }
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
    
    public static Comparator<String> naturalOrderComparator() {
        return (s1, s2) -> {
            if (s1 == null && s2 == null) return 0;
            if (s1 == null) return -1;
            if (s2 == null) return 1;
            
            List<Object> parts1 = splitIntoParts(s1);
            List<Object> parts2 = splitIntoParts(s2);
            
            int minLength = Math.min(parts1.size(), parts2.size());
            for (int i = 0; i < minLength; i++) {
                Object part1 = parts1.get(i);
                Object part2 = parts2.get(i);
                
                int result;
                if (part1 instanceof Long && part2 instanceof Long) {
                    result = ((Long) part1).compareTo((Long) part2);
                } else {
                    result = part1.toString().compareToIgnoreCase(part2.toString());
                }
                
                if (result != 0) {
                    return result;
                }
            }
            
            return Integer.compare(parts1.size(), parts2.size());
        };
    }
    
    private static List<Object> splitIntoParts(String str) {
        List<Object> parts = new ArrayList<>();
        Matcher matcher = NUMBER_PATTERN.matcher(str);
        int lastEnd = 0;
        
        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                parts.add(str.substring(lastEnd, matcher.start()));
            }
            try {
                parts.add(Long.parseLong(matcher.group()));
            } catch (NumberFormatException e) {
                parts.add(matcher.group());
            }
            lastEnd = matcher.end();
        }
        
        if (lastEnd < str.length()) {
            parts.add(str.substring(lastEnd));
        }
        
        return parts;
    }
}

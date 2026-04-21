package com.solo.video.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilTest {
    
    @Test
    void testIsBlank() {
        assertTrue(StringUtil.isBlank(null));
        assertTrue(StringUtil.isBlank(""));
        assertTrue(StringUtil.isBlank(" "));
        assertTrue(StringUtil.isBlank("   "));
        
        assertFalse(StringUtil.isBlank("a"));
        assertFalse(StringUtil.isBlank(" abc "));
    }
    
    @Test
    void testIsNotBlank() {
        assertFalse(StringUtil.isNotBlank(null));
        assertFalse(StringUtil.isNotBlank(""));
        assertFalse(StringUtil.isNotBlank(" "));
        
        assertTrue(StringUtil.isNotBlank("a"));
        assertTrue(StringUtil.isNotBlank(" abc "));
    }
    
    @Test
    void testTruncate() {
        assertNull(StringUtil.truncate(null, 10));
        assertEquals("short", StringUtil.truncate("short", 10));
        assertEquals("exact", StringUtil.truncate("exact", 5));
        assertEquals("long...", StringUtil.truncate("longstring", 7));
        assertEquals("exactly10", StringUtil.truncate("exactly10", 10));
    }
    
    @Test
    void testFormatDateTime() {
        assertEquals("", StringUtil.formatDateTime(null));
        
        LocalDateTime dateTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        String formatted = StringUtil.formatDateTime(dateTime);
        
        assertTrue(formatted.contains("2024"));
        assertTrue(formatted.contains("01"));
        assertTrue(formatted.contains("15"));
        assertTrue(formatted.contains("10"));
        assertTrue(formatted.contains("30"));
    }
    
    @Test
    void testSanitizeFileName() {
        assertNull(StringUtil.sanitizeFileName(null));
        assertEquals("normal_file.txt", StringUtil.sanitizeFileName("normal_file.txt"));
        assertEquals("_invalid_path_file.txt", StringUtil.sanitizeFileName("/invalid/path/file.txt"));
        assertEquals("test_file_txt", StringUtil.sanitizeFileName("test:file.txt"));
        assertEquals("_file_name_", StringUtil.sanitizeFileName("\\file*name?"));
    }
}

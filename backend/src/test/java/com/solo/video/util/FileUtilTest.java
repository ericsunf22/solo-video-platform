package com.solo.video.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilTest {
    
    @Test
    void testGetFileExtension() {
        assertEquals("mp4", FileUtil.getFileExtension("video.mp4"));
        assertEquals("avi", FileUtil.getFileExtension("test.video.avi"));
        assertEquals("", FileUtil.getFileExtension("noextension"));
        assertEquals("", FileUtil.getFileExtension(""));
        assertEquals("", FileUtil.getFileExtension(null));
    }
    
    @Test
    void testIsVideoFile_String() {
        assertTrue(FileUtil.isVideoFile("video.mp4"));
        assertTrue(FileUtil.isVideoFile("video.MP4"));
        assertTrue(FileUtil.isVideoFile("video.avi"));
        assertTrue(FileUtil.isVideoFile("video.mkv"));
        assertTrue(FileUtil.isVideoFile("video.mov"));
        assertTrue(FileUtil.isVideoFile("video.flv"));
        assertTrue(FileUtil.isVideoFile("video.wmv"));
        assertTrue(FileUtil.isVideoFile("video.webm"));
        assertTrue(FileUtil.isVideoFile("video.m4v"));
        
        assertFalse(FileUtil.isVideoFile("document.pdf"));
        assertFalse(FileUtil.isVideoFile("image.jpg"));
        assertFalse(FileUtil.isVideoFile(""));
        assertFalse(FileUtil.isVideoFile(null));
    }
    
    @Test
    void testIsVideoFile_File(@TempDir Path tempDir) throws IOException {
        Path mp4File = tempDir.resolve("video.mp4");
        Files.createFile(mp4File);
        
        Path txtFile = tempDir.resolve("document.txt");
        Files.createFile(txtFile);
        
        assertTrue(FileUtil.isVideoFile(mp4File.toFile()));
        assertFalse(FileUtil.isVideoFile(txtFile.toFile()));
        assertFalse(FileUtil.isVideoFile(null));
        assertFalse(FileUtil.isVideoFile(new File("/nonexistent/path")));
    }
    
    @Test
    void testGetFileNameWithoutExtension() {
        assertEquals("video", FileUtil.getFileNameWithoutExtension("video.mp4"));
        assertEquals("test.video", FileUtil.getFileNameWithoutExtension("test.video.avi"));
        assertEquals("noextension", FileUtil.getFileNameWithoutExtension("noextension"));
        assertEquals("", FileUtil.getFileNameWithoutExtension(""));
        assertEquals("", FileUtil.getFileNameWithoutExtension(null));
    }
    
    @Test
    void testFormatFileSize() {
        assertEquals("0 B", FileUtil.formatFileSize(0L));
        assertEquals("500 B", FileUtil.formatFileSize(500L));
        assertEquals("1.00 KB", FileUtil.formatFileSize(1024L));
        assertEquals("1.50 KB", FileUtil.formatFileSize(1536L));
        assertEquals("1.00 MB", FileUtil.formatFileSize(1024L * 1024L));
        assertEquals("1.00 GB", FileUtil.formatFileSize(1024L * 1024L * 1024L));
        assertEquals("1.00 TB", FileUtil.formatFileSize(1024L * 1024L * 1024L * 1024L));
    }
}

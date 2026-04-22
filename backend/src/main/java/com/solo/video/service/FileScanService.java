package com.solo.video.service;

import com.solo.video.dto.response.ScanResultResponse;

public interface FileScanService {
    
    ScanResultResponse scanFolder(String folderPath, boolean recursive, boolean updateExisting);
    
    ScanResultResponse scanFolder(String folderPath);
    
    int getScanProgress();
    
    boolean isScanning();
    
    void cancelScan();
    
    String getCurrentScanningFile();
    
    int getNewVideosCount();
    
    int getUpdatedVideosCount();
    
    int getSkippedVideosCount();
}

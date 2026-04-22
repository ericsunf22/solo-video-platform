package com.solo.video.service;

import java.util.List;

public interface CoverIntegrityService {
    
    IntegrityCheckResult checkIntegrity();
    
    int cleanOrphanCovers();
    
    int repairMissingCovers(boolean forceRegenerate);
    
    boolean isCheckRunning();
    
    record IntegrityCheckResult(
        int totalVideos,
        int videosWithValidCover,
        int videosWithMissingCover,
        int totalCoverFiles,
        int orphanCoverFiles,
        List<String> missingCovers,
        List<String> orphanCovers
    ) {}
}

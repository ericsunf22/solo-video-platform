package com.solo.video.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUploadResult {
    
    @Builder.Default
    private List<VideoResponse> successes = new ArrayList<>();
    
    @Builder.Default
    private List<UploadFailure> failures = new ArrayList<>();
    
    private int totalCount;
    private int successCount;
    private int failureCount;
    
    public boolean hasFailures() {
        return failureCount > 0;
    }
    
    public boolean isAllSuccessful() {
        return failureCount == 0 && totalCount > 0;
    }
}

package com.solo.video.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlayProgressRequest {
    
    @NotNull(message = "视频ID不能为空")
    private Long videoId;
    
    @NotNull(message = "播放进度不能为空")
    private Long progress;
    
    private Long duration;
}

package com.solo.video.exception;

public class VideoNotFoundException extends BusinessException {
    public VideoNotFoundException(Long videoId) {
        super(404, "视频不存在: " + videoId);
    }
}

package com.solo.video.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VideoUpdateRequest {
    
    @Size(max = 500, message = "标题不能超过500个字符")
    private String title;
    
    @Size(max = 2000, message = "描述不能超过2000个字符")
    private String description;
}

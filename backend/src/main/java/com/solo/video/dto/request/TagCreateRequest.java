package com.solo.video.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TagCreateRequest {
    
    @NotBlank(message = "标签名称不能为空")
    @Size(max = 100, message = "标签名称不能超过100个字符")
    private String name;
    
    @Size(max = 20, message = "颜色代码不能超过20个字符")
    private String color = "#3b82f6";
    
    @Size(max = 500, message = "描述不能超过500个字符")
    private String description;
}

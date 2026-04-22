package com.solo.video.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FolderScanRequest {
    
    @NotBlank(message = "文件夹路径不能为空")
    private String folderPath;
    
    private Boolean recursive = true;
    
    private Boolean updateExisting = false;
}

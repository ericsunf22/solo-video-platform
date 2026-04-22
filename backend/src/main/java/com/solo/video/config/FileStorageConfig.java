package com.solo.video.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Data
@Validated
@ConfigurationProperties(prefix = "app.file")
public class FileStorageConfig {
    private Storage storage = new Storage();
    private Scan scan = new Scan();
    
    @Data
    public static class Storage {
        private String path = "./storage/videos";
        private String coverPath = "./storage/covers";
        private String tempPath = "./storage/temp";
    }
    
    @Data
    public static class Scan {
        @NotEmpty
        private List<String> supportedFormats = List.of("mp4", "avi", "mkv", "mov", "flv", "wmv", "webm", "m4v");
    }
}

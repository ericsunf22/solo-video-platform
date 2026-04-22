package com.solo.video;

import com.solo.video.config.FileStorageConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(FileStorageConfig.class)
@EnableScheduling
public class VideoPlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(VideoPlatformApplication.class, args);
    }
}

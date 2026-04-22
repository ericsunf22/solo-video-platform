package com.solo.video.controller;

import com.solo.video.dto.response.ApiResponse;
import com.solo.video.service.AppSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingController {
    
    private final AppSettingService appSettingService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> getAllSettings() {
        Map<String, String> settings = appSettingService.getAllSettings();
        return ResponseEntity.ok(ApiResponse.success(settings));
    }
    
    @GetMapping("/{key}")
    public ResponseEntity<ApiResponse<String>> getSetting(
            @PathVariable String key,
            @RequestParam(required = false) String defaultValue
    ) {
        String value = appSettingService.getSetting(key, defaultValue);
        return ResponseEntity.ok(ApiResponse.success(value));
    }
    
    @PutMapping("/{key}")
    public ResponseEntity<ApiResponse<Void>> setSetting(
            @PathVariable String key,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        String value = body != null && body.containsKey("value") ? String.valueOf(body.get("value")) : null;
        String description = body != null && body.containsKey("description") ? String.valueOf(body.get("description")) : null;
        
        if (value == null) {
            appSettingService.deleteSetting(key);
        } else {
            appSettingService.setSetting(key, value, description);
        }
        
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    @DeleteMapping("/{key}")
    public ResponseEntity<ApiResponse<Void>> deleteSetting(@PathVariable String key) {
        appSettingService.deleteSetting(key);
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    @PutMapping
    public ResponseEntity<ApiResponse<Void>> setSettings(@RequestBody Map<String, String> settings) {
        appSettingService.setSettings(settings);
        return ResponseEntity.ok(ApiResponse.success());
    }
    
    public static class SettingRequest {
        private String value;
        private String description;
        
        public String getValue() {
            return value;
        }
        
        public void setValue(String value) {
            this.value = value;
        }
        
        public String getDescription() {
            return description;
        }
        
        public void setDescription(String description) {
            this.description = description;
        }
    }
}

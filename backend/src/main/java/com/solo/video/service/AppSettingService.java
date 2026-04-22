package com.solo.video.service;

import java.util.Map;
import java.util.Optional;

public interface AppSettingService {
    
    Optional<String> getSetting(String key);
    
    String getSetting(String key, String defaultValue);
    
    <T> Optional<T> getSetting(String key, Class<T> type);
    
    void setSetting(String key, String value);
    
    void setSetting(String key, String value, String description);
    
    void setSettings(Map<String, String> settings);
    
    void deleteSetting(String key);
    
    boolean hasSetting(String key);
    
    Map<String, String> getAllSettings();
}

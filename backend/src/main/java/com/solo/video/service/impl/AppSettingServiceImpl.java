package com.solo.video.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.solo.video.entity.AppSetting;
import com.solo.video.repository.AppSettingRepository;
import com.solo.video.service.AppSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppSettingServiceImpl implements AppSettingService {
    
    private final AppSettingRepository appSettingRepository;
    private final ObjectMapper objectMapper;
    
    @Override
    public Optional<String> getSetting(String key) {
        return appSettingRepository.findByKey(key)
                .map(AppSetting::getValue);
    }
    
    @Override
    public String getSetting(String key, String defaultValue) {
        return getSetting(key).orElse(defaultValue);
    }
    
    @Override
    public <T> Optional<T> getSetting(String key, Class<T> type) {
        return getSetting(key).flatMap(value -> {
            try {
                if (type == String.class) {
                    return Optional.of(type.cast(value));
                }
                return Optional.ofNullable(objectMapper.readValue(value, type));
            } catch (Exception e) {
                log.error("Failed to parse setting value for key: {}", key, e);
                return Optional.empty();
            }
        });
    }
    
    @Override
    @Transactional
    public void setSetting(String key, String value) {
        setSetting(key, value, null);
    }
    
    @Override
    @Transactional
    public void setSetting(String key, String value, String description) {
        Optional<AppSetting> existing = appSettingRepository.findByKey(key);
        
        if (existing.isPresent()) {
            AppSetting setting = existing.get();
            setting.setValue(value);
            if (description != null) {
                setting.setDescription(description);
            }
            appSettingRepository.save(setting);
            log.debug("Updated setting: {} = {}", key, value);
        } else {
            AppSetting setting = new AppSetting();
            setting.setKey(key);
            setting.setValue(value);
            setting.setDescription(description);
            setting.setType("STRING");
            appSettingRepository.save(setting);
            log.debug("Created setting: {} = {}", key, value);
        }
    }
    
    @Override
    @Transactional
    public void setSettings(Map<String, String> settings) {
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            setSetting(entry.getKey(), entry.getValue());
        }
    }
    
    @Override
    @Transactional
    public void deleteSetting(String key) {
        appSettingRepository.deleteByKey(key);
        log.debug("Deleted setting: {}", key);
    }
    
    @Override
    public boolean hasSetting(String key) {
        return appSettingRepository.existsByKey(key);
    }
    
    @Override
    public Map<String, String> getAllSettings() {
        Map<String, String> settings = new HashMap<>();
        appSettingRepository.findAll().forEach(setting -> {
            settings.put(setting.getKey(), setting.getValue());
        });
        return settings;
    }
}

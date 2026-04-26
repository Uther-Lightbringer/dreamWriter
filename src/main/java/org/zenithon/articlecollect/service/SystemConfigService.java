package org.zenithon.articlecollect.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zenithon.articlecollect.entity.SystemConfig;
import org.zenithon.articlecollect.repository.SystemConfigRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 系统配置服务
 */
@Service
public class SystemConfigService {

    private static final Logger logger = LoggerFactory.getLogger(SystemConfigService.class);

    private final SystemConfigRepository repository;

    public SystemConfigService(SystemConfigRepository repository) {
        this.repository = repository;
    }

    /**
     * 获取配置值
     */
    public String getConfigValue(String key, String defaultValue) {
        return repository.findByConfigKey(key)
                .map(SystemConfig::getConfigValue)
                .orElse(defaultValue);
    }

    /**
     * 获取配置值（无默认值）
     */
    public Optional<String> getConfigValue(String key) {
        return repository.findByConfigKey(key)
                .map(SystemConfig::getConfigValue);
    }

    /**
     * 设置配置值
     */
    @Transactional
    public void setConfigValue(String key, String value, String description) {
        SystemConfig config = repository.findByConfigKey(key)
                .orElse(new SystemConfig(key, value, description));
        config.setConfigValue(value);
        config.setDescription(description);
        config.setUpdatedAt(LocalDateTime.now());
        repository.save(config);
        logger.info("配置已更新: {} = {}", key, value);
    }

    /**
     * 设置配置值（使用现有描述）
     */
    @Transactional
    public void setConfigValue(String key, String value) {
        SystemConfig config = repository.findByConfigKey(key)
                .orElseGet(() -> new SystemConfig(key, value, null));
        config.setConfigValue(value);
        config.setUpdatedAt(LocalDateTime.now());
        repository.save(config);
        logger.info("配置已更新: {} = {}", key, value);
    }

    /**
     * 初始化默认配置（如果不存在）
     */
    @Transactional
    public void initDefaultConfig(String key, String value, String description) {
        if (repository.findByConfigKey(key).isEmpty()) {
            SystemConfig config = new SystemConfig(key, value, description);
            repository.save(config);
            logger.info("初始化默认配置: {} = {}", key, value);
        }
    }

    /**
     * 获取所有配置
     */
    public List<SystemConfig> getAllConfigs() {
        return repository.findAll();
    }

    /**
     * 删除配置
     */
    @Transactional
    public void deleteConfig(String key) {
        repository.deleteByConfigKey(key);
        logger.info("配置已删除: {}", key);
    }
}

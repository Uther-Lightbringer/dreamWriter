package org.zenithon.articlecollect.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zenithon.articlecollect.dto.DeepSeekConfigDTO;
import org.zenithon.articlecollect.dto.DeepSeekRuntimeConfig;
import org.zenithon.articlecollect.entity.DeepSeekFeatureConfig;
import org.zenithon.articlecollect.entity.DeepSeekFeatureConfig.FeatureCode;
import org.zenithon.articlecollect.repository.DeepSeekFeatureConfigRepository;

import java.util.List;

/**
 * DeepSeek 配置服务
 */
@Service
public class DeepSeekConfigService {

    private static final Logger logger = LoggerFactory.getLogger(DeepSeekConfigService.class);

    private final DeepSeekFeatureConfigRepository repository;

    public DeepSeekConfigService(DeepSeekFeatureConfigRepository repository) {
        this.repository = repository;
    }

    /**
     * 初始化默认配置
     * 如果配置表为空，插入初始数据
     */
    @Transactional
    public void initializeDefaultConfigs() {
        if (repository.count() == 0) {
            logger.info("初始化 DeepSeek 功能配置...");

            // 创作引导：Pro + 思考模式
            DeepSeekFeatureConfig creativeGuidance = new DeepSeekFeatureConfig(FeatureCode.CREATIVE_GUIDANCE);
            creativeGuidance.setModel("deepseek-v4-pro");
            creativeGuidance.setThinkingEnabled(true);
            creativeGuidance.setReasoningEffort("high");
            repository.save(creativeGuidance);

            // AI 聊天：Flash，无思考模式
            DeepSeekFeatureConfig aiChat = new DeepSeekFeatureConfig(FeatureCode.AI_CHAT);
            repository.save(aiChat);

            // 提示词生成：Flash，无思考模式
            DeepSeekFeatureConfig promptGeneration = new DeepSeekFeatureConfig(FeatureCode.PROMPT_GENERATION);
            repository.save(promptGeneration);

            logger.info("DeepSeek 功能配置初始化完成，共 {} 条", 3);
        }
    }

    /**
     * 获取所有功能配置
     */
    public List<DeepSeekFeatureConfig> getAllConfigs() {
        return repository.findAllOrderById();
    }

    /**
     * 获取单个功能配置
     */
    public DeepSeekFeatureConfig getConfig(FeatureCode featureCode) {
        return repository.findByFeatureCode(featureCode)
                .orElseThrow(() -> new RuntimeException("配置不存在: " + featureCode));
    }

    /**
     * 更新功能配置
     */
    @Transactional
    public DeepSeekFeatureConfig updateConfig(FeatureCode featureCode, DeepSeekConfigDTO dto) {
        DeepSeekFeatureConfig config = getConfig(featureCode);

        if (dto.getModel() != null) {
            config.setModel(dto.getModel());
        }
        if (dto.getThinkingEnabled() != null) {
            config.setThinkingEnabled(dto.getThinkingEnabled());
        }
        if (dto.getReasoningEffort() != null) {
            config.setReasoningEffort(dto.getReasoningEffort());
        }

        DeepSeekFeatureConfig saved = repository.save(config);
        logger.info("更新 DeepSeek 配置: {} -> model={}, thinking={}, effort={}",
                featureCode, saved.getModel(), saved.getThinkingEnabled(), saved.getReasoningEffort());

        return saved;
    }

    /**
     * 获取运行时配置（合并数据库默认值和前端传入的覆盖值）
     *
     * @param featureCode 功能代码
     * @param override    前端传入的覆盖配置（可为 null）
     * @return 合并后的运行时配置
     */
    public DeepSeekRuntimeConfig getRuntimeConfig(FeatureCode featureCode, DeepSeekConfigDTO override) {
        DeepSeekFeatureConfig defaultConfig = getConfig(featureCode);

        DeepSeekRuntimeConfig.Builder builder = DeepSeekRuntimeConfig.builder();

        // 如果有覆盖配置，优先使用覆盖值
        if (override != null) {
            builder.model(override.getModel() != null ? override.getModel() : defaultConfig.getModel());
            builder.thinkingEnabled(override.getThinkingEnabled() != null ? override.getThinkingEnabled() : defaultConfig.getThinkingEnabled());
            builder.reasoningEffort(override.getReasoningEffort() != null ? override.getReasoningEffort() : defaultConfig.getReasoningEffort());

            logger.debug("运行时配置: {} (覆盖)", featureCode);
        } else {
            // 使用默认配置
            builder.model(defaultConfig.getModel());
            builder.thinkingEnabled(defaultConfig.getThinkingEnabled());
            builder.reasoningEffort(defaultConfig.getReasoningEffort());

            logger.debug("运行时配置: {} (默认)", featureCode);
        }

        DeepSeekRuntimeConfig runtime = builder.build();
        logger.info("DeepSeek 运行时配置: {} -> {}", featureCode, runtime);

        return runtime;
    }

    /**
     * 获取功能的默认运行时配置
     */
    public DeepSeekRuntimeConfig getDefaultRuntimeConfig(FeatureCode featureCode) {
        return getRuntimeConfig(featureCode, null);
    }
}

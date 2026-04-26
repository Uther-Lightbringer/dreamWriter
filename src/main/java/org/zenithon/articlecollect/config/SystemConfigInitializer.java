package org.zenithon.articlecollect.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.zenithon.articlecollect.service.SystemConfigService;

/**
 * 系统配置初始化器
 */
@Component
public class SystemConfigInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SystemConfigInitializer.class);

    private final SystemConfigService configService;

    public SystemConfigInitializer(SystemConfigService configService) {
        this.configService = configService;
    }

    @Override
    public void run(String... args) {
        logger.info("初始化系统配置...");

        // 初始化图片模型配置
        configService.initDefaultConfig("image.model", "z-image-turbo", "当前使用的文生图模型");
        configService.initDefaultConfig("image.available_models", "z-image-turbo,gpt-image-2", "可用模型列表");

        logger.info("系统配置初始化完成");
    }
}

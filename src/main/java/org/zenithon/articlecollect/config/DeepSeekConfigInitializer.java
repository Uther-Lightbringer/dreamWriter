package org.zenithon.articlecollect.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.zenithon.articlecollect.service.DeepSeekConfigService;

/**
 * DeepSeek 配置初始化器
 *
 * 在应用启动时检查并初始化默认配置
 */
@Component
public class DeepSeekConfigInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DeepSeekConfigInitializer.class);

    private final DeepSeekConfigService deepSeekConfigService;

    public DeepSeekConfigInitializer(DeepSeekConfigService deepSeekConfigService) {
        this.deepSeekConfigService = deepSeekConfigService;
    }

    @Override
    public void run(String... args) {
        logger.info("检查 DeepSeek 功能配置...");
        deepSeekConfigService.initializeDefaultConfigs();
    }
}

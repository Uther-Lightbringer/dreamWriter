package org.zenithon.articlecollect.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * DeepSeek AI 配置类
 */
@Component
public class DeepSeekConfig {

    @Value("${deepseek.api.key:}")
    private String apiKey;

    @Value("${deepseek.api.url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${deepseek.api.model:}")
    private String configuredModel;

    // Pro 模型优惠截止时间：2026/05/05 23:59 北京时间
    private static final LocalDateTime PROMO_END_TIME = LocalDateTime.of(2026, 5, 5, 23, 59, 0);

    /**
     * 获取默认模型
     * 优惠期内默认使用 Pro，优惠期后默认使用 Flash
     */
    public String getDefaultModel() {
        // 如果配置文件中明确指定了模型，则使用配置的模型
        if (configuredModel != null && !configuredModel.isEmpty()) {
            return configuredModel;
        }

        // 判断是否在优惠期内
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        if (now.isBefore(PROMO_END_TIME)) {
            return "deepseek-v4-pro";
        } else {
            return "deepseek-v4-flash";
        }
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getModel() {
        return configuredModel != null && !configuredModel.isEmpty() ? configuredModel : getDefaultModel();
    }

    public void setModel(String model) {
        this.configuredModel = model;
    }

    /**
     * 判断当前是否在 Pro 优惠期内
     */
    public boolean isPromoActive() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        return now.isBefore(PROMO_END_TIME);
    }
}

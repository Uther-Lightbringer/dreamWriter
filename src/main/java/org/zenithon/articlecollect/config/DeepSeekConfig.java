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

    @Value("${deepseek.api.max-tokens:8192}")
    private int maxTokens;

    // Pro 模型优惠截止时间：2026/05/05 23:59 北京时间
    private static final LocalDateTime PROMO_END_TIME = LocalDateTime.of(2026, 5, 5, 23, 59, 0);

    // 默认使用 Flash（非思考模式）
    private static final String DEFAULT_MODEL = "deepseek-v4-flash";
    private static final String PRO_MODEL = "deepseek-v4-pro";

    // DeepSeek API 最大输出 token 限制
    public static final int MAX_TOKENS_LIMIT = 8192;

    /**
     * 获取当前使用的模型
     * 优先级：配置文件 > 默认值（Flash）
     */
    public String getModel() {
        // 如果配置文件中明确指定了模型，则使用配置的模型
        if (configuredModel != null && !configuredModel.isEmpty()) {
            return configuredModel;
        }
        return DEFAULT_MODEL;
    }

    /**
     * 获取默认模型（非思考模式）
     */
    public String getDefaultModel() {
        return DEFAULT_MODEL;
    }

    /**
     * 获取 Pro 模型（思考模式）
     */
    public String getProModel() {
        return PRO_MODEL;
    }

    /**
     * 获取最大输出 tokens
     * 限制在 API 允许范围内（最大 8192）
     */
    public int getMaxTokens() {
        if (maxTokens <= 0 || maxTokens > MAX_TOKENS_LIMIT) {
            return MAX_TOKENS_LIMIT;
        }
        return maxTokens;
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

    public void setModel(String model) {
        this.configuredModel = model;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    /**
     * 判断当前是否在 Pro 优惠期内
     */
    public boolean isPromoActive() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Shanghai"));
        return now.isBefore(PROMO_END_TIME);
    }

    /**
     * 判断当前是否使用思考模式（Pro 模型）
     */
    public boolean isThinkingMode() {
        String model = getModel();
        return PRO_MODEL.equals(model) || (model != null && model.contains("pro"));
    }
}

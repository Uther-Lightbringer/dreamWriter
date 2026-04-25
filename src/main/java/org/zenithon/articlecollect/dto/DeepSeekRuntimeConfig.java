package org.zenithon.articlecollect.dto;

/**
 * DeepSeek 运行时配置
 *
 * 用于 API 调用的配置快照，合并数据库默认值和前端覆盖值
 */
public class DeepSeekRuntimeConfig {

    private String model;
    private Boolean thinkingEnabled;
    private String reasoningEffort;

    // 默认构造函数
    public DeepSeekRuntimeConfig() {
        this.model = "deepseek-v4-flash";
        this.thinkingEnabled = false;
        this.reasoningEffort = "high";
    }

    // Builder 模式
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String model = "deepseek-v4-flash";
        private Boolean thinkingEnabled = false;
        private String reasoningEffort = "high";

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder thinkingEnabled(Boolean thinkingEnabled) {
            this.thinkingEnabled = thinkingEnabled;
            return this;
        }

        public Builder reasoningEffort(String reasoningEffort) {
            this.reasoningEffort = reasoningEffort;
            return this;
        }

        public DeepSeekRuntimeConfig build() {
            DeepSeekRuntimeConfig config = new DeepSeekRuntimeConfig();
            config.model = this.model;
            config.thinkingEnabled = this.thinkingEnabled;
            config.reasoningEffort = this.reasoningEffort;
            return config;
        }
    }

    // 从 DTO 创建
    public static DeepSeekRuntimeConfig fromDTO(DeepSeekConfigDTO dto) {
        DeepSeekRuntimeConfig config = new DeepSeekRuntimeConfig();
        if (dto.getModel() != null) {
            config.model = dto.getModel();
        }
        if (dto.getThinkingEnabled() != null) {
            config.thinkingEnabled = dto.getThinkingEnabled();
        }
        if (dto.getReasoningEffort() != null) {
            config.reasoningEffort = dto.getReasoningEffort();
        }
        return config;
    }

    // Getters and Setters

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Boolean getThinkingEnabled() {
        return thinkingEnabled;
    }

    public void setThinkingEnabled(Boolean thinkingEnabled) {
        this.thinkingEnabled = thinkingEnabled;
    }

    public String getReasoningEffort() {
        return reasoningEffort;
    }

    public void setReasoningEffort(String reasoningEffort) {
        this.reasoningEffort = reasoningEffort;
    }

    @Override
    public String toString() {
        return "DeepSeekRuntimeConfig{" +
                "model='" + model + '\'' +
                ", thinkingEnabled=" + thinkingEnabled +
                ", reasoningEffort='" + reasoningEffort + '\'' +
                '}';
    }
}

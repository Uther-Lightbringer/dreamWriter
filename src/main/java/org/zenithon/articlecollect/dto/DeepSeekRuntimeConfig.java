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
    private Double temperature;
    private Double topP;
    private Double frequencyPenalty;
    private Double presencePenalty;

    // 默认构造函数
    public DeepSeekRuntimeConfig() {
        this.model = "deepseek-v4-flash";
        this.thinkingEnabled = false;
        this.reasoningEffort = "high";
        this.temperature = 0.0;
        this.topP = 0.1;
        this.frequencyPenalty = 1.0;
        this.presencePenalty = -2.0;
    }

    // Builder 模式
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String model = "deepseek-v4-flash";
        private Boolean thinkingEnabled = false;
        private String reasoningEffort = "high";
        private Double temperature = 0.0;
        private Double topP = 0.1;
        private Double frequencyPenalty = 1.0;
        private Double presencePenalty = -2.0;

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

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public Builder frequencyPenalty(Double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        public Builder presencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        public DeepSeekRuntimeConfig build() {
            DeepSeekRuntimeConfig config = new DeepSeekRuntimeConfig();
            config.model = this.model;
            config.thinkingEnabled = this.thinkingEnabled;
            config.reasoningEffort = this.reasoningEffort;
            config.temperature = this.temperature;
            config.topP = this.topP;
            config.frequencyPenalty = this.frequencyPenalty;
            config.presencePenalty = this.presencePenalty;
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
        if (dto.getTemperature() != null) {
            config.temperature = dto.getTemperature();
        }
        if (dto.getTopP() != null) {
            config.topP = dto.getTopP();
        }
        if (dto.getFrequencyPenalty() != null) {
            config.frequencyPenalty = dto.getFrequencyPenalty();
        }
        if (dto.getPresencePenalty() != null) {
            config.presencePenalty = dto.getPresencePenalty();
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

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getTopP() {
        return topP;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    public Double getFrequencyPenalty() {
        return frequencyPenalty;
    }

    public void setFrequencyPenalty(Double frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
    }

    public Double getPresencePenalty() {
        return presencePenalty;
    }

    public void setPresencePenalty(Double presencePenalty) {
        this.presencePenalty = presencePenalty;
    }

    @Override
    public String toString() {
        return "DeepSeekRuntimeConfig{" +
                "model='" + model + '\'' +
                ", thinkingEnabled=" + thinkingEnabled +
                ", reasoningEffort='" + reasoningEffort + '\'' +
                ", temperature=" + temperature +
                ", topP=" + topP +
                ", frequencyPenalty=" + frequencyPenalty +
                ", presencePenalty=" + presencePenalty +
                '}';
    }
}

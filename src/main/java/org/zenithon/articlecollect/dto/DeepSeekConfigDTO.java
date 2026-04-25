package org.zenithon.articlecollect.dto;

/**
 * DeepSeek 配置 DTO
 *
 * 用于前端显示和更新配置
 */
public class DeepSeekConfigDTO {

    private String featureCode;
    private String featureName;
    private String model;
    private Boolean thinkingEnabled;
    private String reasoningEffort;
    private String description;

    // 标记是否为运行时覆盖（前端传入，不持久化）
    private Boolean isRuntimeOverride;

    // 默认构造函数
    public DeepSeekConfigDTO() {}

    // 从实体转换
    public static DeepSeekConfigDTO fromEntity(org.zenithon.articlecollect.entity.DeepSeekFeatureConfig entity) {
        DeepSeekConfigDTO dto = new DeepSeekConfigDTO();
        dto.setFeatureCode(entity.getFeatureCode().name());
        dto.setFeatureName(entity.getFeatureName());
        dto.setModel(entity.getModel());
        dto.setThinkingEnabled(entity.getThinkingEnabled());
        dto.setReasoningEffort(entity.getReasoningEffort());
        dto.setDescription(entity.getDescription());
        dto.setIsRuntimeOverride(false);
        return dto;
    }

    // Getters and Setters

    public String getFeatureCode() {
        return featureCode;
    }

    public void setFeatureCode(String featureCode) {
        this.featureCode = featureCode;
    }

    public String getFeatureName() {
        return featureName;
    }

    public void setFeatureName(String featureName) {
        this.featureName = featureName;
    }

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsRuntimeOverride() {
        return isRuntimeOverride;
    }

    public void setIsRuntimeOverride(Boolean isRuntimeOverride) {
        this.isRuntimeOverride = isRuntimeOverride;
    }
}

package org.zenithon.articlecollect.dto;

import java.util.List;

/**
 * 图片生成请求 DTO（支持所有模型参数）
 */
public class ImageGenerationRequest {

    private String prompt;
    private String size;

    // z-image-turbo 专用参数
    private Integer seed;

    // gpt-image-2 专用参数
    private String resolution;
    private String quality;
    private Integer n;
    private List<String> imageUrls;

    // doubao-seedream-4.0 专用参数
    private String promptPriority;

    public ImageGenerationRequest() {
    }

    // Getters and Setters
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public Integer getSeed() { return seed; }
    public void setSeed(Integer seed) { this.seed = seed; }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }
    public Integer getN() { return n; }
    public void setN(Integer n) { this.n = n; }
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public String getPromptPriority() { return promptPriority; }
    public void setPromptPriority(String promptPriority) { this.promptPriority = promptPriority; }
}

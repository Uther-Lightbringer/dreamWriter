package org.zenithon.articlecollect.dto;

import java.util.List;

/**
 * 模型能力 DTO
 */
public class ModelCapability {

    private String model;
    private boolean supportsResolution;
    private boolean supportsQuality;
    private boolean supportsBatch;
    private boolean supportsImageToImage;
    private boolean supportsSeed;
    private List<String> supportedSizes;
    private int maxPromptLength;
    private int minPixel;
    private int maxPixel;

    public ModelCapability() {
    }

    // Getters and Setters
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public boolean isSupportsResolution() { return supportsResolution; }
    public void setSupportsResolution(boolean supportsResolution) { this.supportsResolution = supportsResolution; }
    public boolean isSupportsQuality() { return supportsQuality; }
    public void setSupportsQuality(boolean supportsQuality) { this.supportsQuality = supportsQuality; }
    public boolean isSupportsBatch() { return supportsBatch; }
    public void setSupportsBatch(boolean supportsBatch) { this.supportsBatch = supportsBatch; }
    public boolean isSupportsImageToImage() { return supportsImageToImage; }
    public void setSupportsImageToImage(boolean supportsImageToImage) { this.supportsImageToImage = supportsImageToImage; }
    public boolean isSupportsSeed() { return supportsSeed; }
    public void setSupportsSeed(boolean supportsSeed) { this.supportsSeed = supportsSeed; }
    public List<String> getSupportedSizes() { return supportedSizes; }
    public void setSupportedSizes(List<String> supportedSizes) { this.supportedSizes = supportedSizes; }
    public int getMaxPromptLength() { return maxPromptLength; }
    public void setMaxPromptLength(int maxPromptLength) { this.maxPromptLength = maxPromptLength; }
    public int getMinPixel() { return minPixel; }
    public void setMinPixel(int minPixel) { this.minPixel = minPixel; }
    public int getMaxPixel() { return maxPixel; }
    public void setMaxPixel(int maxPixel) { this.maxPixel = maxPixel; }
}

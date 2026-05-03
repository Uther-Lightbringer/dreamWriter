package org.zenithon.articlecollect.service;

import org.springframework.stereotype.Service;
import org.zenithon.articlecollect.dto.ModelCapability;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图片模型能力服务
 */
@Service
public class ImageModelCapabilityService {

    private final Map<String, ModelCapability> capabilities = new HashMap<>();

    public ImageModelCapabilityService() {
        initCapabilities();
    }

    private void initCapabilities() {
        // z-image-turbo 能力
        ModelCapability zImageTurbo = new ModelCapability();
        zImageTurbo.setModel("z-image-turbo");
        zImageTurbo.setSupportsResolution(false);
        zImageTurbo.setSupportsQuality(false);
        zImageTurbo.setSupportsBatch(false);
        zImageTurbo.setSupportsImageToImage(false);
        zImageTurbo.setSupportsSeed(true);
        zImageTurbo.setSupportedSizes(Arrays.asList(
            "1:1", "2:3", "3:2", "3:4", "4:3", "9:16", "16:9", "1:2", "2:1"
        ));
        zImageTurbo.setMaxPromptLength(4000);
        zImageTurbo.setMinPixel(376);
        zImageTurbo.setMaxPixel(1536);
        capabilities.put("z-image-turbo", zImageTurbo);

        // gpt-image-2 能力
        ModelCapability gptImage2 = new ModelCapability();
        gptImage2.setModel("gpt-image-2");
        gptImage2.setSupportsResolution(true);
        gptImage2.setSupportsQuality(true);
        gptImage2.setSupportsBatch(true);
        gptImage2.setSupportsImageToImage(true);
        gptImage2.setSupportsSeed(false);
        gptImage2.setSupportsPromptPriority(false);
        gptImage2.setSupportedSizes(Arrays.asList(
            "auto", "1:1", "2:3", "3:2", "3:4", "4:3", "9:16", "16:9", "1:2", "2:1",
            "4:5", "5:4", "9:21", "21:9", "1:3", "3:1"
        ));
        gptImage2.setMaxPromptLength(32000);
        gptImage2.setMinPixel(16);
        gptImage2.setMaxPixel(3840);
        capabilities.put("gpt-image-2", gptImage2);

        // doubao-seedream-4.0 能力
        ModelCapability seedream4 = new ModelCapability();
        seedream4.setModel("doubao-seedream-4.0");
        seedream4.setSupportsResolution(false);
        seedream4.setSupportsQuality(true);
        seedream4.setSupportsBatch(true);
        seedream4.setSupportsImageToImage(true);
        seedream4.setSupportsSeed(false);
        seedream4.setSupportsPromptPriority(true);
        seedream4.setSupportedSizes(Arrays.asList(
            "auto", "1:1", "2:3", "3:2", "3:4", "4:3", "4:5", "5:4", "9:16", "16:9", "21:9"
        ));
        seedream4.setMaxPromptLength(2000);
        seedream4.setMinPixel(1);
        seedream4.setMaxPixel(6000);
        capabilities.put("doubao-seedream-4.0", seedream4);

        // doubao-seedream-4.5 能力
        ModelCapability seedream45 = new ModelCapability();
        seedream45.setModel("doubao-seedream-4.5");
        seedream45.setSupportsResolution(false);
        seedream45.setSupportsQuality(true);
        seedream45.setSupportsBatch(true);
        seedream45.setSupportsImageToImage(true);
        seedream45.setSupportsSeed(false);
        seedream45.setSupportsPromptPriority(true);
        seedream45.setSupportedSizes(Arrays.asList(
            "auto", "1:1", "2:3", "3:2", "3:4", "4:3", "4:5", "5:4", "9:16", "16:9", "21:9"
        ));
        seedream45.setMaxPromptLength(2000);
        seedream45.setMinPixel(1);
        seedream45.setMaxPixel(6000);
        capabilities.put("doubao-seedream-4.5", seedream45);
    }

    /**
     * 获取指定模型的能力
     */
    public ModelCapability getCapability(String modelName) {
        return capabilities.getOrDefault(modelName, capabilities.get("z-image-turbo"));
    }

    /**
     * 获取所有可用模型名称
     */
    public List<String> getAvailableModels() {
        return Arrays.asList("z-image-turbo", "gpt-image-2", "doubao-seedream-4.0", "doubao-seedream-4.5");
    }

    /**
     * 检查模型是否有效
     */
    public boolean isValidModel(String modelName) {
        return capabilities.containsKey(modelName);
    }
}

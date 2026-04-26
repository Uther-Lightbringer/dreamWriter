package org.zenithon.articlecollect.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zenithon.articlecollect.dto.ImageModelConfig;
import org.zenithon.articlecollect.dto.ModelCapability;
import org.zenithon.articlecollect.service.SystemConfigService;
import org.zenithon.articlecollect.service.ImageModelCapabilityService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统配置控制器
 */
@RestController
@RequestMapping("/api/config")
public class SystemConfigController {

    private static final String IMAGE_MODEL_KEY = "image.model";
    private static final String DEFAULT_MODEL = "z-image-turbo";

    private final SystemConfigService configService;
    private final ImageModelCapabilityService capabilityService;

    public SystemConfigController(SystemConfigService configService,
                                   ImageModelCapabilityService capabilityService) {
        this.configService = configService;
        this.capabilityService = capabilityService;
    }

    /**
     * 获取当前图片模型配置
     */
    @GetMapping("/image-model")
    public ResponseEntity<Map<String, Object>> getImageModelConfig() {
        String currentModel = configService.getConfigValue(IMAGE_MODEL_KEY, DEFAULT_MODEL);

        List<ImageModelConfig> availableModels = capabilityService.getAvailableModels().stream()
                .map(this::createModelConfig)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("currentModel", currentModel);
        response.put("availableModels", availableModels);

        return ResponseEntity.ok(response);
    }

    /**
     * 更新图片模型配置
     */
    @PutMapping("/image-model")
    public ResponseEntity<Map<String, Object>> updateImageModel(@RequestBody Map<String, String> request) {
        String model = request.get("model");

        if (model == null || model.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "模型名称不能为空");
            return ResponseEntity.badRequest().body(error);
        }

        if (!capabilityService.isValidModel(model)) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "无效的模型名称: " + model);
            return ResponseEntity.badRequest().body(error);
        }

        configService.setConfigValue(IMAGE_MODEL_KEY, model, "当前使用的文生图模型");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("currentModel", model);
        response.put("message", "模型已切换为 " + model);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取当前模型的能力信息
     */
    @GetMapping("/image-model/capability")
    public ResponseEntity<ModelCapability> getModelCapability() {
        String currentModel = configService.getConfigValue(IMAGE_MODEL_KEY, DEFAULT_MODEL);
        ModelCapability capability = capabilityService.getCapability(currentModel);
        return ResponseEntity.ok(capability);
    }

    /**
     * 获取指定模型的能力信息
     */
    @GetMapping("/image-model/capability/{modelName}")
    public ResponseEntity<ModelCapability> getModelCapabilityByName(@PathVariable String modelName) {
        ModelCapability capability = capabilityService.getCapability(modelName);
        return ResponseEntity.ok(capability);
    }

    private ImageModelConfig createModelConfig(String modelId) {
        switch (modelId) {
            case "z-image-turbo":
                return new ImageModelConfig(modelId, "Z Image Turbo", "快速生成，支持一致性种子");
            case "gpt-image-2":
                return new ImageModelConfig(modelId, "GPT Image 2", "高质量生成，支持图生图和 4K 分辨率");
            default:
                return new ImageModelConfig(modelId, modelId, "");
        }
    }
}

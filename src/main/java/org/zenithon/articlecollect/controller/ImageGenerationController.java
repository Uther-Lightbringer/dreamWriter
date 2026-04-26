package org.zenithon.articlecollect.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zenithon.articlecollect.dto.ImageHistoryRequest;
import org.zenithon.articlecollect.dto.ModelCapability;
import org.zenithon.articlecollect.entity.AiImageHistory;
import org.zenithon.articlecollect.service.AiImageHistoryService;
import org.zenithon.articlecollect.service.EvoLinkImageService;
import org.zenithon.articlecollect.service.ImageModelCapabilityService;
import org.zenithon.articlecollect.service.SystemConfigService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图片生成控制器
 */
@RestController
@RequestMapping("/api/image")
public class ImageGenerationController {

    private final EvoLinkImageService imageService;
    private final AiImageHistoryService historyService;
    private final SystemConfigService configService;
    private final ImageModelCapabilityService capabilityService;

    public ImageGenerationController(EvoLinkImageService imageService,
                                      AiImageHistoryService historyService,
                                      SystemConfigService configService,
                                      ImageModelCapabilityService capabilityService) {
        this.imageService = imageService;
        this.historyService = historyService;
        this.configService = configService;
        this.capabilityService = capabilityService;
    }
    
    /**
     * 创建图片生成任务
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateImage(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String prompt = (String) request.get("prompt");
            String size = (String) request.get("size");
            Integer seed = request.get("seed") != null ? ((Number) request.get("seed")).intValue() : null;
            String resolution = (String) request.get("resolution");
            String quality = (String) request.get("quality");
            Integer n = request.get("n") != null ? ((Number) request.get("n")).intValue() : null;
            @SuppressWarnings("unchecked")
            List<String> imageUrls = (List<String>) request.get("imageUrls");

            if (prompt == null || prompt.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "提示词不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            // 默认尺寸 16:9
            if (size == null || size.trim().isEmpty()) {
                size = "16:9";
            }

            // 验证尺寸格式
            if (!validateSize(size)) {
                response.put("success", false);
                response.put("error", "无效的尺寸格式。支持比例：1:1, 2:3, 3:2, 3:4, 4:3, 9:16, 16:9, 1:2, 2:1 或自定义尺寸如 1024x768");
                return ResponseEntity.badRequest().body(response);
            }

            // 获取当前模型和能力
            String currentModel = configService.getConfigValue("image.model", "z-image-turbo");
            ModelCapability capability = capabilityService.getCapability(currentModel);

            // 根据模型能力过滤参数
            Integer effectiveSeed = capability.isSupportsSeed() ? seed : null;

            // 调用图片生成服务
            String taskId = imageService.generateImage(prompt, size, effectiveSeed);

            response.put("success", true);
            response.put("taskId", taskId);
            response.put("model", currentModel);
            response.put("message", "图片生成任务已创建");

            // 如果有参数被忽略，添加提示
            if (!capability.isSupportsSeed() && seed != null) {
                response.put("warning", "当前模型不支持 seed 参数，已忽略");
            }
            if (!capability.isSupportsResolution() && resolution != null) {
                response.put("warning", "当前模型不支持 resolution 参数，已忽略");
            }
            if (!capability.isSupportsQuality() && quality != null) {
                response.put("warning", "当前模型不支持 quality 参数，已忽略");
            }
            if (!capability.isSupportsBatch() && n != null && n > 1) {
                response.put("warning", "当前模型不支持批量生成，已忽略 n 参数");
            }
            if (!capability.isSupportsImageToImage() && imageUrls != null && !imageUrls.isEmpty()) {
                response.put("warning", "当前模型不支持图生图，已忽略 imageUrls 参数");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 查询任务状态
     */
    @GetMapping("/status/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskStatus(@PathVariable String taskId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            EvoLinkImageService.TaskStatus status = imageService.getTaskStatus(taskId);
            
            response.put("success", true);
            response.put("taskId", status.getId());
            response.put("status", status.getStatus());
            response.put("progress", status.getProgress());
            
            if (status.isCompleted()) {
                response.put("imageUrl", status.getImageUrl());
            } else if (status.isFailed()) {
                response.put("error", status.getError());
            }
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取当前图片模型信息
     */
    @GetMapping("/model-info")
    public ResponseEntity<Map<String, Object>> getModelInfo() {
        String currentModel = configService.getConfigValue("image.model", "z-image-turbo");
        ModelCapability capability = capabilityService.getCapability(currentModel);

        Map<String, Object> response = new HashMap<>();
        response.put("model", currentModel);
        response.put("capabilities", capability);

        return ResponseEntity.ok(response);
    }

    /**
     * 验证尺寸格式
     */
    private boolean validateSize(String size) {
        // 检查比例格式 (如 16:9)
        if (size.contains(":")) {
            String[] validRatios = {"1:1", "2:3", "3:2", "3:4", "4:3", "9:16", "16:9", "1:2", "2:1"};
            for (String ratio : validRatios) {
                if (ratio.equals(size)) {
                    return true;
                }
            }
            return false;
        }
        
        // 检查自定义尺寸格式 (如 1024x768)
        if (size.contains("x")) {
            try {
                String[] parts = size.split("x");
                if (parts.length != 2) {
                    return false;
                }
                
                int width = Integer.parseInt(parts[0]);
                int height = Integer.parseInt(parts[1]);
                
                // 验证范围 376-1536
                return width >= 376 && width <= 1536 && height >= 376 && height <= 1536;
                
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * 保存图片历史记录
     */
    @PostMapping("/history")
    public ResponseEntity<Map<String, Object>> saveHistory(@RequestBody ImageHistoryRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (request.getPrompt() == null || request.getImageUrl() == null) {
                response.put("success", false);
                response.put("error", "参数不完整");
                return ResponseEntity.badRequest().body(response);
            }

            AiImageHistory history = historyService.saveHistory(
                request.getPrompt(),
                request.getImageUrl(),
                request.getNovelId(),
                request.getNovelTitle(),
                request.getChapterId(),
                request.getChapterTitle(),
                request.getNovelContent()
            );

            if (history != null) {
                response.put("success", true);
                response.put("message", "保存成功");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("error", "保存失败");
                return ResponseEntity.internalServerError().body(response);
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取图片历史记录
     */
    @GetMapping("/history")
    public ResponseEntity<List<AiImageHistory>> getHistory() {
        try {
            List<AiImageHistory> history = historyService.getAllHistory();
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 删除图片历史记录
     */
    @DeleteMapping("/history/{id}")
    public ResponseEntity<Map<String, Object>> deleteHistory(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            historyService.deleteHistory(id);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}

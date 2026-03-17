package org.zenithon.articlecollect.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zenithon.articlecollect.service.EvoLinkImageService;

import java.util.HashMap;
import java.util.Map;

/**
 * 图片生成控制器
 */
@RestController
@RequestMapping("/api/image")
public class ImageGenerationController {
    
    private final EvoLinkImageService imageService;
    
    public ImageGenerationController(EvoLinkImageService imageService) {
        this.imageService = imageService;
    }
    
    /**
     * 创建图片生成任务
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateImage(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String prompt = request.get("prompt");
            String size = request.get("size");
            
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
            
            String taskId = imageService.generateImage(prompt, size);
            
            response.put("success", true);
            response.put("taskId", taskId);
            response.put("message", "图片生成任务已创建");
            
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
}

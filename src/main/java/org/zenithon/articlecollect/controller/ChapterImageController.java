package org.zenithon.articlecollect.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zenithon.articlecollect.service.ChapterImageService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 章节自动配图控制器
 */
@RestController
@RequestMapping("/api/chapter-image")
public class ChapterImageController {
    
    private static final Logger logger = LoggerFactory.getLogger(ChapterImageController.class);
    
    private final ChapterImageService chapterImageService;
    
    public ChapterImageController(ChapterImageService chapterImageService) {
        this.chapterImageService = chapterImageService;
    }
    
    /**
     * 分析章节内容，找出适合配图的位置
     */
    @GetMapping("/analyze/{chapterId}")
    public ResponseEntity<Map<String, Object>> analyzeChapter(@PathVariable Long chapterId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Map<String, Object>> positions = chapterImageService.analyzeChapterForImages(chapterId);
            
            response.put("success", true);
            response.put("positions", positions);
            response.put("count", positions.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("分析章节失败：" + e.getMessage(), e);
            response.put("success", false);
            response.put("error", "分析失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 为单个位置生成图片
     */
    @PostMapping("/generate-single")
    public ResponseEntity<Map<String, Object>> generateSingleImage(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long chapterId = Long.valueOf(request.get("chapterId").toString());
            String description = (String) request.get("description");
            
            if (description == null || description.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "描述不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 生成提示词
            String prompt = chapterImageService.generateImagePrompt(description);
            
            if (prompt == null || prompt.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "提示词生成失败");
                return ResponseEntity.internalServerError().body(response);
            }
            
            response.put("success", true);
            response.put("prompt", prompt);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("生成单个图片失败：" + e.getMessage(), e);
            response.put("success", false);
            response.put("error", "生成失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 执行完整的自动配图流程（支持用户选择位置）
     */
    @PostMapping("/auto-generate/{chapterId}")
    public ResponseEntity<Map<String, Object>> autoGenerateImages(@PathVariable Long chapterId,
                                                                   @RequestBody(required = false) Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            logger.info("开始为章节 {} 执行自动配图流程", chapterId);
            
            // 如果用户选择了特定位置，则只生成这些位置
            List<Integer> selectedPositions = null;
            if (request != null && request.containsKey("selectedPositions")) {
                selectedPositions = (List<Integer>) request.get("selectedPositions");
                logger.info("用户选择了 {} 个位置进行生成", selectedPositions.size());
            }
            
            // 执行自动配图
            List<Map<String, Object>> generatedImages = chapterImageService.autoGenerateImagesForChapter(chapterId, selectedPositions);
            
            if (generatedImages.isEmpty()) {
                response.put("success", true);
                response.put("message", "未找到合适的配图位置或生成失败");
                response.put("images", generatedImages);
            } else {
                response.put("success", true);
                response.put("message", "成功生成 " + generatedImages.size() + " 张图片");
                response.put("images", generatedImages);
                response.put("count", generatedImages.size());
            }
            
            logger.info("自动配图完成，生成 {} 张图片", generatedImages.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("自动配图失败：" + e.getMessage(), e);
            response.put("success", false);
            response.put("error", "自动配图失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 将图片插入到章节内容中
     */
    @PostMapping("/insert")
    public ResponseEntity<Map<String, Object>> insertImagesToContent(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String content = (String) request.get("content");
            List<Map<String, Object>> images = (List<Map<String, Object>>) request.get("images");
            
            if (content == null || images == null) {
                response.put("success", false);
                response.put("error", "参数不完整");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 插入图片到内容
            String newContent = chapterImageService.insertImagesToContent(content, images);
            
            response.put("success", true);
            response.put("newContent", newContent);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("插入图片失败：" + e.getMessage(), e);
            response.put("success", false);
            response.put("error", "插入失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}

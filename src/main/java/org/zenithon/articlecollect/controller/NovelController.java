package org.zenithon.articlecollect.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zenithon.articlecollect.entity.Novel;
import org.zenithon.articlecollect.entity.Chapter;
import org.zenithon.articlecollect.entity.ChapterDetailView;
import org.zenithon.articlecollect.service.NovelService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 小说管理REST控制器
 */
@RestController
@RequestMapping("/api")
public class NovelController {
    
    @Autowired
    private NovelService novelService;
    
    /**
     * 创建新小说
     * POST /api/novels
     * 请求体: {"title": "小说标题", "author": "作者名", "description": "小说描述"} (author和description可选)
     * 返回: {"id": 1, "title": "小说标题", "author": "作者名", "description": "小说描述", "createTime": "...", "updateTime": "..."}
     */
    @PostMapping("/novels")
    public ResponseEntity<Novel> createNovel(@RequestBody Map<String, String> request) {
        String title = request.get("title");
        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        String author = request.get("author");
        String description = request.get("description");
        
        Novel novel;
        if (author != null && !author.trim().isEmpty()) {
            if (description != null && !description.trim().isEmpty()) {
                novel = novelService.createNovel(title.trim(), author.trim(), description.trim());
            } else {
                novel = novelService.createNovel(title.trim(), author.trim());
            }
        } else {
            novel = novelService.createNovel(title.trim());
        }
        return ResponseEntity.ok(novel);
    }
    
    /**
     * 获取所有小说
     * GET /api/novels
     * 返回: 小说列表
     */
    @GetMapping("/novels")
    public ResponseEntity<List<Novel>> getAllNovels() {
        List<Novel> novels = novelService.getAllNovels();
        return ResponseEntity.ok(novels);
    }
    
    /**
     * 根据ID获取小说
     * GET /api/novels/{novelId}
     * 返回: 小说详情
     */
    @GetMapping("/novels/{novelId}")
    public ResponseEntity<Novel> getNovel(@PathVariable Long novelId) {
        Novel novel = novelService.getNovelById(novelId);
        if (novel == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(novel);
    }
    
    /**
     * 删除小说
     * DELETE /api/novels/{novelId}
     * 返回: {"success": true/false}
     */
    @DeleteMapping("/novels/{novelId}")
    public ResponseEntity<Map<String, Object>> deleteNovel(@PathVariable Long novelId) {
        boolean success = novelService.deleteNovel(novelId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 为小说创建章节
     * POST /api/novels/{novelId}/chapters
     * 请求体: {"title": "章节标题", "content": "章节内容"}
     * 返回: 章节详情
     */
    @PostMapping("/novels/{novelId}/chapters")
    public ResponseEntity<?> createChapter(
            @PathVariable Long novelId,
            @RequestBody Map<String, String> request) {
        
        try {
            String title = request.get("title");
            String content = request.get("content");
            String index = request.get("index");
            String storySummary = request.get("storySummary"); // 新增故事概括参数
            
            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("章节标题不能为空");
            }

            Chapter chapter = novelService.createChapter(novelId, title.trim(), content, Long.valueOf(index), storySummary);

            
            return ResponseEntity.ok(chapter);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    
    /**
     * 获取小说的所有章节
     * GET /api/novels/{novelId}/chapters
     * 返回: 章节列表
     */
    @GetMapping("/novels/{novelId}/chapters")
    public ResponseEntity<List<Chapter>> getChapters(@PathVariable Long novelId) {
        // 验证小说是否存在
        if (novelService.getNovelById(novelId) == null) {
            return ResponseEntity.notFound().build();
        }
        
        List<Chapter> chapters = novelService.getChaptersByNovelId(novelId);
        return ResponseEntity.ok(chapters);
    }
    
    /**
     * 获取特定章节
     * GET /api/novels/{novelId}/chapters/{chapterId}
     * 返回: 章节详情
     */
    @GetMapping("/novel/{novelId}/chapter/{chapterId}")
    public ResponseEntity<Chapter> getChapter(
            @PathVariable Long novelId,
            @PathVariable Long chapterId) {
        
        Chapter chapter = novelService.getChapterById(chapterId);
        if (chapter == null || !chapter.getNovelId().equals(novelId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(chapter);
    }
    
    /**
     * 获取章节详情（包含导航信息）
     * GET /api/chapters/detail/{novelId}/{chapterId}
     * 返回: 章节详情视图
     */
    @GetMapping("/chapters/detail/{novelId}/{chapterId}")
    public ResponseEntity<ChapterDetailView> getChapterDetailView(
            @PathVariable Long novelId,
            @PathVariable Long chapterId) {
        
        ChapterDetailView detailView = novelService.getChapterDetailView(novelId, chapterId);
        if (detailView == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(detailView);
    }
    
    /**
     * 更新章节
     * PUT /api/novel/{novelId}/chapter/{chapterId}
     * 请求体: {"title": "新标题", "content": "新内容"}
     * 返回: 更新后的章节
     */
    @PutMapping("/novels/{novelId}/chapters/{chapterId}")
    public ResponseEntity<?> updateChapter(
            @PathVariable Long novelId,
            @PathVariable Long chapterId,
            @RequestBody Map<String, String> request) {
        
        Chapter chapter = novelService.getChapterById(chapterId);
        if (chapter == null || !chapter.getNovelId().equals(novelId)) {
            return ResponseEntity.notFound().build();
        }
        
        String title = request.get("title");
        String content = request.get("content");
        
        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("章节标题不能为空");
        }
        
        Chapter updatedChapter = novelService.updateChapter(chapterId, title.trim(), content);
        return ResponseEntity.ok(updatedChapter);
    }
    
    /**
     * 删除章节
     * DELETE /api/novels/{novelId}/chapters/{chapterId}
     * 返回: {"success": true/false, "message": "..."}
     */
    @DeleteMapping("/novels/{novelId}/chapters/{chapterId}")
    public ResponseEntity<Map<String, Object>> deleteChapterByNovel(
            @PathVariable Long novelId,
            @PathVariable Long chapterId) {
        
        Chapter chapter = novelService.getChapterById(chapterId);
        if (chapter == null || !chapter.getNovelId().equals(novelId)) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "章节不存在或不属于该小说");
            return ResponseEntity.status(404).body(response);
        }
        
        boolean success = novelService.deleteChapter(chapterId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "章节删除成功" : "章节删除失败");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 根据章节ID获取章节详情
     * GET /api/chapters/{chapterId}
     * 返回: 章节详情
     */
    @GetMapping("/chapters/{chapterId}")
    public ResponseEntity<Chapter> getChapterById(@PathVariable Long chapterId) {
        Chapter chapter = novelService.getChapterById(chapterId);
        if (chapter == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(chapter);
    }
    
    /**
     * 更新章节内容
     * PUT /api/chapters/{chapterId}
     * 请求体: {"title": "新标题", "content": "新内容"}
     * 返回: {"success": true/false, "message": "..."}
     */
    @PutMapping("/chapters/{chapterId}")
    public ResponseEntity<Map<String, Object>> updateChapter(
            @PathVariable Long chapterId,
            @RequestBody Map<String, String> request) {
        
        Chapter chapter = novelService.getChapterById(chapterId);
        if (chapter == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "章节不存在");
            return ResponseEntity.status(404).body(response);
        }
        
        String title = request.get("title");
        String content = request.get("content");
        
        if (title == null || title.trim().isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "章节标题不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            Chapter updatedChapter = novelService.updateChapter(chapterId, title.trim(), content);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "章节更新成功");
            response.put("data", updatedChapter);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "章节更新失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 删除章节（通过章节ID）
     * DELETE /api/chapters/{chapterId}
     * 返回: {"success": true/false, "message": "..."}
     */
    @DeleteMapping("/chapters/{chapterId}")
    public ResponseEntity<Map<String, Object>> deleteChapter(
            @PathVariable Long chapterId) {
        
        Chapter chapter = novelService.getChapterById(chapterId);
        if (chapter == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "章节不存在");
            return ResponseEntity.status(404).body(response);
        }
        
        boolean success = novelService.deleteChapter(chapterId);
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "章节删除成功" : "章节删除失败");
        return ResponseEntity.ok(response);
    }
}
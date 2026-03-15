package org.zenithon.articlecollect.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zenithon.articlecollect.entity.CharacterCardEntity;
import org.zenithon.articlecollect.entity.Novel;
import org.zenithon.articlecollect.entity.Chapter;
import org.zenithon.articlecollect.entity.ChapterDetailView;
import org.zenithon.articlecollect.service.CharacterCardAsyncService;
import org.zenithon.articlecollect.service.CharacterCardService;
import org.zenithon.articlecollect.service.NovelService;
import org.zenithon.articlecollect.service.CharacterCardPromptTaskService;
import org.zenithon.articlecollect.dto.WorldViewRequest;
import org.zenithon.articlecollect.dto.CharacterCardRequest;
import org.zenithon.articlecollect.dto.CharacterCard;
import org.zenithon.articlecollect.dto.CharacterCardBatchRequest;

import java.util.*;

/**
 * 小说管理REST控制器
 */
@RestController
@Slf4j
@RequestMapping("/api")
public class NovelController {
    
    @Autowired
    private NovelService novelService;

    @Autowired
    private CharacterCardAsyncService characterCardAsyncService;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CharacterCardService characterCardService;
    
    @Autowired
    private CharacterCardPromptTaskService promptTaskService;
    
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
    
    /**
     * 更新小说的世界观
     * PUT /api/novels/{novelId}/worldview
     * 请求体：{"worldView": "世界观内容（支持 Markdown 格式）"}
     * 返回：{"success": true, "message": "...", "data": {更新后的小说对象}}
     */
    @PutMapping("/novels/{novelId}/worldview")
    public ResponseEntity<Map<String, Object>> updateWorldView(
            @PathVariable Long novelId,
            @RequestBody WorldViewRequest request) {
        
        Novel novel = novelService.getNovelById(novelId);
        if (novel == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "小说不存在，ID: " + novelId);
            return ResponseEntity.status(404).body(response);
        }
        
        try {
            String worldView = request.getWorldView();
            Novel updatedNovel = novelService.updateWorldView(novelId, worldView);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "世界观更新成功");
            response.put("data", updatedNovel);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "世界观更新失败：" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取小说的世界观
     * GET /api/novels/{novelId}/worldview
     * 返回：{"success": true, "worldView": "世界观内容"}
     */
    @GetMapping("/novels/{novelId}/worldview")
    public ResponseEntity<Map<String, Object>> getWorldView(@PathVariable Long novelId) {
        Novel novel = novelService.getNovelById(novelId);
        if (novel == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "小说不存在，ID: " + novelId);
            return ResponseEntity.status(404).body(response);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("worldView", novel.getWorldView() != null ? novel.getWorldView() : "");
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取小说的角色卡
     * GET /api/novels/{novelId}/character-cards
     * 返回：{"success": true, "characterCards": "角色卡内容"}
     */
    @GetMapping("/novels/{novelId}/character-cards")
    public ResponseEntity<Map<String, Object>> getCharacterCards(@PathVariable Long novelId) {
        Novel novel = novelService.getNovelById(novelId);
        if (novel == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "小说不存在，ID: " + novelId);
            return ResponseEntity.status(404).body(response);
        }
        
        try {
            List<CharacterCard> characterCardsList = novelService.getCharacterCardsList(novelId);
            
            // 将列表转换为 JSON 字符串返回（保持向后兼容）
            String json = objectMapper.writeValueAsString(characterCardsList);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("characterCards", json);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取角色卡失败：" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 批量更新小说的角色卡（结构化数据）
     * POST /api/novels/{novelId}/character-cards/batch
     * 请求体：[{角色卡对象}, {角色卡对象}...]
     * 返回：{"success": true, "message": "...", "data": {更新后的小说对象}}
     */
    @PostMapping("/novels/{novelId}/character-cards/batch")
    public ResponseEntity<Map<String, Object>> updateCharacterCardsBatch(
            @PathVariable Long novelId,
            @RequestBody List<CharacterCard> characterCards) {
        
        Novel novel = novelService.getNovelById(novelId);
        if (novel == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "小说不存在，ID: " + novelId);
            return ResponseEntity.status(404).body(response);
        }
        
        try {
            // 直接使用前端传入的 ID（批量导入时前端不需要传 ID）
            characterCards = novelService.saveCharacterCardsToDatabase(novelId, characterCards);

            // 启动异步任务处理 AI 绘画提示词生成和图片生成
            characterCardAsyncService.processCharacterCardsAsync(novelId, characterCards);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "角色卡更新成功");
            response.put("data", characterCards);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "角色卡更新失败：" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 生成唯一的角色 ID
     * 格式：char-{novelId}-{timestamp}-{index}
     */
    private String generateUniqueCharacterId(Long novelId, int index) {
        long timestamp = System.currentTimeMillis();
        return String.format("char-%d-%d-%d", novelId, timestamp, index);
    }
    
    /**
     * 获取小说的角色卡列表（结构化数据）
     * GET /api/novels/{novelId}/character-cards/list
     * 返回：{"success": true, "characterCards": [{角色卡对象}...]}
     */
    @GetMapping("/novels/{novelId}/character-cards/list")
    public ResponseEntity<Map<String, Object>> getCharacterCardsList(@PathVariable Long novelId) {
        Novel novel = novelService.getNovelById(novelId);
        if (novel == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "小说不存在，ID: " + novelId);
            return ResponseEntity.status(404).body(response);
        }
        
        try {
            List<CharacterCard> characterCardsList = novelService.getCharacterCardsList(novelId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("characterCards", characterCardsList);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取角色卡失败：" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 添加单个角色卡
     * POST /api/novels/{novelId}/character-cards
     * 返回：{"success": true, "message": "...", "data": {角色卡对象}}
     */
    @PostMapping("/novels/{novelId}/character-cards")
    public ResponseEntity<Map<String, Object>> addSingleCharacterCard(
            @PathVariable Long novelId,
            @RequestBody CharacterCard characterCard) {
        
        Novel novel = novelService.getNovelById(novelId);
        if (novel == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "小说不存在，ID: " + novelId);
            return ResponseEntity.status(404).body(response);
        }
        
        try {
            // 获取现有角色卡列表
            List<CharacterCard> existingCards = novelService.getCharacterCardsList(novelId);
            
            // 添加到列表
            existingCards.add(characterCard);
            
            // 保存到数据库（ID 由数据库自动生成）
            characterCardService.saveCharacterCards(novelId, existingCards);

            // 重新从数据库获取最新的角色卡列表（包含自动生成的 ID）
            List<CharacterCard> cardsWithIds = characterCardService.getCharacterCardsByNovelId(novelId);

            // 启动异步任务处理 AI 绘画提示词生成和图片生成（使用带 ID 的数据）
            characterCardAsyncService.processCharacterCardsAsync(novelId, cardsWithIds);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "角色卡添加成功");
            response.put("data", characterCard);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "添加角色卡失败：" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 保存单个角色卡（新增或更新）
     * POST /api/novels/{novelId}/character-cards/save
     * 返回：{"success": true, "message": "...", "data": {保存后的角色卡}}
     */
    @PostMapping("/novels/{novelId}/character-cards/save")
    public ResponseEntity<Map<String, Object>> saveSingleCharacterCard(
            @PathVariable Long novelId,
            @RequestBody CharacterCard characterCard) {
        
        Novel novel = novelService.getNovelById(novelId);
        if (novel == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "小说不存在，ID: " + novelId);
            return ResponseEntity.status(404).body(response);
        }
        
        try {
            // 验证必填字段
            if (characterCard.getName() == null || characterCard.getName().trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "角色姓名不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 保存角色卡
            CharacterCard savedCard = novelService.saveSingleCharacterCard(novelId, characterCard);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "角色卡保存成功");
            response.put("data", savedCard);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "保存角色卡失败：" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 删除单个角色卡
     * DELETE /api/novels/{novelId}/character-cards/{characterId}
     * 返回：{"success": true, "message": "..."}
     */
    @DeleteMapping("/novels/{novelId}/character-cards/{characterId}")
    public ResponseEntity<Map<String, Object>> deleteCharacterCard(
            @PathVariable Long novelId,
            @PathVariable Long characterId) {
        
        Novel novel = novelService.getNovelById(novelId);
        if (novel == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "小说不存在，ID: " + novelId);
            return ResponseEntity.status(404).body(response);
        }
        
        try {
            // 直接删除角色卡
            characterCardService.deleteCharacterCard(characterId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "角色卡删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "删除角色卡失败：" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 为单个角色卡重新生成 AI 绘画提示词（异步 + 长轮询）
     * POST /api/novels/{novelId}/character-cards/{characterId}/generate-prompt
     * 立即返回：{"success": true, "message": "已发送提示词生成请求", "taskId": "xxx"}
     */
    @PostMapping("/novels/{novelId}/character-cards/{characterId}/generate-prompt")
    public ResponseEntity<Map<String, Object>> regenerateAIPrompt(
            @PathVariable Long novelId,
            @PathVariable Long characterId) {
            
        Novel novel = novelService.getNovelById(novelId);
        if (novel == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "小说不存在，ID: " + novelId);
            return ResponseEntity.status(404).body(response);
        }
        try {
            // 获取角色卡信息
            CharacterCard card = characterCardService.getCharacterCardById(characterId);
            if (card == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "角色卡不存在，ID: " + characterId);
                return ResponseEntity.status(404).body(response);
            }
            log.info("正在为角色卡重新生成提示词，characterId={}, 角色名称:{}", characterId, card.getName());

            characterCardService.updateCharacterCardAIGeneratedFields(characterId,
                card.getAppearanceDescription(), card.getGeneratedImageUrl());
            
            // 创建任务
            CharacterCardPromptTaskService.TaskStatus taskStatus = 
                promptTaskService.createTask(novelId, characterId, card.getName());
            
            // 异步执行任务
            characterCardAsyncService.regenerateAIPromptAsync(
                taskStatus.getTaskId(), characterId, novelId);
            
            log.info("已发送提示词生成请求，taskId={}", taskStatus.getTaskId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "已发送提示词生成请求，正在后台处理...");
            response.put("taskId", taskStatus.getTaskId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "发送请求失败：" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 长轮询查询任务状态
     * GET /api/novels/{novelId}/character-cards/tasks/{taskId}/status?timeout=30
     * 阻塞等待最多 timeout 秒，直到任务完成或超时
     */
    @GetMapping("/novels/{novelId}/character-cards/tasks/{taskId}/status")
    public ResponseEntity<Map<String, Object>> getTaskStatus(
            @PathVariable Long novelId,
            @PathVariable String taskId,
            @RequestParam(value = "timeout", defaultValue = "30") int timeout) {
        
        Novel novel = novelService.getNovelById(novelId);
        if (novel == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "小说不存在，ID: " + novelId);
            return ResponseEntity.status(404).body(response);
        }
        
        try {
            // 等待任务完成（长轮询）
            CharacterCardPromptTaskService.TaskStatus status = 
                promptTaskService.waitForTaskCompletion(taskId, timeout);
            
            if (status == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "任务不存在：" + taskId);
                return ResponseEntity.status(404).body(response);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", status.getTaskId());
            response.put("state", status.getState().name());
            response.put("message", status.getMessage());
            
            // 如果任务完成，返回角色卡数据
            if (status.getState() == CharacterCardPromptTaskService.TaskState.COMPLETED && status.getData() != null) {
                CharacterCard updatedCard = (CharacterCard) status.getData();
                response.put("data", updatedCard);
                
                // 构建通知消息
                String notificationMessage = String.format(
                    "小说《%s》的角色\"%s\"的绘画提示词已重新生成成功",
                    novel.getTitle(),
                    updatedCard.getName()
                );
                response.put("notification", notificationMessage);

                // 清理已完成的任务
                promptTaskService.removeTask(taskId);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "查询任务状态失败：" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 为单个角色卡生成图片
     * POST /api/novels/{novelId}/character-cards/{characterId}/generate-image
     * 返回：{"success": true, "message": "...", "data": {更新后的角色卡}}
     */
    @PostMapping("/novels/{novelId}/character-cards/{characterId}/generate-image")
    public ResponseEntity<Map<String, Object>> generateCharacterImage(
            @PathVariable Long novelId,
            @PathVariable Long characterId) {
        
        Novel novel = novelService.getNovelById(novelId);
        if (novel == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "小说不存在，ID: " + novelId);
            return ResponseEntity.status(404).body(response);
        }
        try {
            // 获取角色卡
            CharacterCard targetCard = characterCardService.getCharacterCardById(characterId);
            log.info("正在为角色卡生成图片，novelId={}, characterId={},角色名称：{}", novelId, characterId, targetCard.getName());

            if (targetCard == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "角色卡不存在，ID: " + characterId);
                return ResponseEntity.status(404).body(response);
            }
            
            // 检查是否有 AI 绘画提示词
            if (targetCard.getAppearanceDescription() == null || targetCard.getAppearanceDescription().trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "请先为角色卡生成 AI 绘画提示词");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 调用火山引擎生成图片
            String imageUrl = novelService.generateImageForCharacter(targetCard);
            
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                // 使用带版本号的更新方法
                CharacterCard updatedCard = characterCardService.regenerateAIImage(
                    characterId, targetCard.getAppearanceDescription(), imageUrl);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "角色图片生成成功");
                response.put("data", updatedCard);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "图片生成失败，请检查火山引擎配置");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "生成图片失败：" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取角色卡的图片
     * GET /api/novels/{novelId}/character-cards/{characterId}/image
     * 返回：{"success": true, "imageUrl": "图片路径"}
     */
    @GetMapping("/novels/{novelId}/character-cards/{characterId}/image")
    public ResponseEntity<Map<String, Object>> getCharacterCardImage(
            @PathVariable Long novelId,
            @PathVariable Long characterId) {
        
        Novel novel = novelService.getNovelById(novelId);
        if (novel == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "小说不存在，ID: " + novelId);
            return ResponseEntity.status(404).body(response);
        }
        
        try {
            // 直接通过 ID 获取角色卡
            CharacterCard targetCard = characterCardService.getCharacterCardById(characterId);
            
            if (targetCard == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "角色卡不存在，ID: " + characterId);
                return ResponseEntity.status(404).body(response);
            }
            
            // 返回角色卡的图片 URL
            Map<String, Object> response = new HashMap<>();
            String imageUrl = targetCard.getGeneratedImageUrl();
            response.put("success", true);
            response.put("imageUrl", imageUrl != null ? imageUrl : "");
            response.put("characterId", characterId);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取角色卡图片失败：" + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
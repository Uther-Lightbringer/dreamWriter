package org.zenithon.articlecollect.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zenithon.articlecollect.entity.Novel;
import org.zenithon.articlecollect.entity.Chapter;
import org.zenithon.articlecollect.entity.ChapterDetailView;
import org.zenithon.articlecollect.service.CharacterCardAsyncService;
import org.zenithon.articlecollect.service.NovelService;
import org.zenithon.articlecollect.dto.WorldViewRequest;
import org.zenithon.articlecollect.dto.CharacterCardRequest;
import org.zenithon.articlecollect.dto.CharacterCard;
import org.zenithon.articlecollect.dto.CharacterCardBatchRequest;

import java.util.*;

/**
 * 小说管理REST控制器
 */
@RestController
@RequestMapping("/api")
public class NovelController {
    
    @Autowired
    private NovelService novelService;

    @Autowired
    private CharacterCardAsyncService characterCardAsyncService;
    
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
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("characterCards", novel.getCharacterCards() != null ? novel.getCharacterCards() : "");
        return ResponseEntity.ok(response);
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
            // 为每个角色卡生成新的唯一 ID，忽略前端传入的 ID
            for (int i = 0; i < characterCards.size(); i++) {
                CharacterCard card = characterCards.get(i);
                String newId = generateUniqueCharacterId(novelId, i);
                card.setId(newId);
            }
            
            Novel updatedNovel = novelService.updateCharacterCardsBatch(novelId, characterCards);

            // 启动异步任务处理 AI 绘画提示词生成和图片生成
            characterCardAsyncService.processCharacterCardsAsync(novelId, characterCards);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "角色卡更新成功");
            response.put("data", updatedNovel);
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
            // 生成新的唯一 ID
            String newId = generateUniqueCharacterId(novelId, 0);
            characterCard.setId(newId);
            
            // 获取现有角色卡列表
            List<CharacterCard> existingCards = novelService.getCharacterCardsList(novelId);
            
            // 添加新角色卡
            existingCards.add(characterCard);
            
            // 保存到数据库
            novelService.updateCharacterCardsBatch(novelId, existingCards);
            characterCardAsyncService.processCharacterCardsAsync(novelId, existingCards);
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
     * 删除单个角色卡
     * DELETE /api/novels/{novelId}/character-cards/{characterId}
     * 返回：{"success": true, "message": "..."}
     */
    @DeleteMapping("/novels/{novelId}/character-cards/{characterId}")
    public ResponseEntity<Map<String, Object>> deleteCharacterCard(
            @PathVariable Long novelId,
            @PathVariable String characterId) {
        
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
            
            // 过滤掉要删除的角色卡
            List<CharacterCard> updatedCards = new ArrayList<>();
            for (CharacterCard card : existingCards) {
                if (!card.getId().equals(characterId)) {
                    updatedCards.add(card);
                }
            }
            
            // 保存更新后的列表
            novelService.updateCharacterCardsBatch(novelId, updatedCards);
            
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
     * 为单个角色卡重新生成 AI 绘画提示词
     * POST /api/novels/{novelId}/character-cards/{characterId}/generate-prompt
     * 返回：{"success": true, "message": "...", "data": {更新后的角色卡}}
     */
    @PostMapping("/novels/{novelId}/character-cards/{characterId}/generate-prompt")
    public ResponseEntity<Map<String, Object>> regenerateAIPrompt(
            @PathVariable Long novelId,
            @PathVariable String characterId) {
        
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
            
            // 找到要更新的角色卡
            CharacterCard targetCard = null;
            for (CharacterCard card : existingCards) {
                if (card.getId().equals(characterId)) {
                    targetCard = card;
                    break;
                }
            }
            
            if (targetCard == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "角色卡不存在，ID: " + characterId);
                return ResponseEntity.status(404).body(response);
            }
            
            // 调用 AI 生成提示词
            String aiPrompt = novelService.generateAIPromptForCharacter(targetCard);
            
            if (aiPrompt != null && !aiPrompt.trim().isEmpty()) {
                targetCard.setAppearanceDescription(aiPrompt);
                
                // 保存更新后的列表
                novelService.updateCharacterCardsBatch(novelId, existingCards);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "AI 绘画提示词生成成功");
                response.put("data", targetCard);
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "AI 提示词生成失败，请检查 API 配置");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "生成 AI 提示词失败：" + e.getMessage());
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
            @PathVariable String characterId) {
        
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
            
            // 找到要更新的角色卡
            CharacterCard targetCard = null;
            for (CharacterCard card : existingCards) {
                if (card.getId().equals(characterId)) {
                    targetCard = card;
                    break;
                }
            }
            
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
                targetCard.setGeneratedImageUrl(imageUrl);
                
                // 保存更新后的列表
                novelService.updateCharacterCardsBatch(novelId, existingCards);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "角色图片生成成功");
                response.put("data", targetCard);
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
            @PathVariable String characterId) {
        
        Novel novel = novelService.getNovelById(novelId);
        if (novel == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "小说不存在，ID: " + novelId);
            return ResponseEntity.status(404).body(response);
        }
        
        try {
            // 获取角色卡列表
            List<CharacterCard> characterCards = novelService.getCharacterCardsList(novelId);
            
            // 找到对应的角色卡
            CharacterCard targetCard = null;
            for (CharacterCard card : characterCards) {
                if (card.getId().equals(characterId)) {
                    targetCard = card;
                    break;
                }
            }
            
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
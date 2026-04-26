package org.zenithon.articlecollect.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.zenithon.articlecollect.entity.Novel;
import org.zenithon.articlecollect.dto.ChapterWithTags;
import org.zenithon.articlecollect.service.NovelService;

import java.util.List;

/**
 * 前端页面控制器
 */
@Controller
public class PageController {
    
    @Autowired
    private NovelService novelService;
    
    /**
     * 首页 - 小说管理页面
     */
    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }
    
    /**
     * 添加小说页面（空页面，用于创建新小说）
     */
    @GetMapping("/novel/add")
    public String addNovel() {
        return "novel-add"; // 对应 novel-add.html 模板
    }
    
    /**
     * 小说详情页面
     */
    @GetMapping("/novel/{novelId}")
    public String novelDetail(@PathVariable Long novelId, Model model) {
        Novel novel = novelService.getNovelById(novelId);
        if (novel == null) {
            // 如果小说不存在，可以重定向到首页或者显示错误页面
            return "redirect:/";
        }
        
        // 获取带标签的章节列表
        List<ChapterWithTags> chaptersWithTags = novelService.getChaptersWithTagsByNovelId(novelId);
        
        model.addAttribute("novel", novel);
        model.addAttribute("novelId", novelId);
        model.addAttribute("chaptersWithTags", chaptersWithTags);
        return "novel-detail";
    }
    
    /**
     * 章节详情页面
     */
    @GetMapping("/novel/{novelId}/chapter/{chapterId}")
    public String chapterDetail(@PathVariable Long novelId, @PathVariable Long chapterId, Model model) {
        model.addAttribute("novelId", novelId);
        model.addAttribute("chapterId", chapterId);
        return "chapter-detail";
    }

    /**
     * 添加章节页面
     */
    @GetMapping("/novel/{novelId}/chapter/add")
    public String addChapter(@PathVariable Long novelId, Model model) {
        Novel novel = novelService.getNovelById(novelId);
        if (novel == null) {
            // 如果小说不存在，重定向到首页
            return "redirect:/";
        }
        model.addAttribute("novel", novel);
        return "add-chapter";
    }

    /**
     * 处理添加章节表单提交
     */
    @PostMapping("/novel/{novelId}/chapter")
    public String createChapter(@PathVariable Long novelId,
                                @RequestParam("title") String title,
                                @RequestParam("content") String content,
                                Model model) {
        try {
            // 创建章节
            novelService.createChapter(novelId, title.trim(), content, null,  null);
            // 重定向到小说详情页
            return "redirect:/novel/" + novelId;
        } catch (Exception e) {
            // 如果创建失败，返回添加章节页面并显示错误
            Novel novel = novelService.getNovelById(novelId);
            if (novel != null) {
                model.addAttribute("novel", novel);
            }
            model.addAttribute("error", "创建章节失败: " + e.getMessage());
            return "add-chapter";
        }
    }
    
    /**
     * 世界观设置页面
     */
    @GetMapping("/worldview/setup")
    public String worldviewSetup() {
        return "worldview-setup";
    }
    
    /**
     * AI 对话页面
     */
    @GetMapping("/ai-chat")
    public String aiChat() {
        return "ai-chat";
    }
    
    /**
     * AI 绘画创作页面
     */
    @GetMapping("/image-generator")
    public String imageGenerator() {
        return "image-generator";
    }
    
    /**
     * AI 图片生成页面（新版）
     */
    @GetMapping("/ai-image-generator")
    public String aiImageGenerator() {
        return "ai-image-generator";
    }

    /**
     * AI 图片历史记录页面
     */
    @GetMapping("/ai-image-history")
    public String aiImageHistory() {
        return "ai-image-history";
    }

    /**
     * DeepSeek 模型配置页面
     */
    @GetMapping("/settings/deepseek")
    public String deepseekSettings() {
        return "settings-deepseek";
    }

    /**
     * 图片模型设置页面
     */
    @GetMapping("/settings/image")
    public String imageSettings() {
        return "settings-image";
    }

    /**
     * 创作引导独立页面
     */
    @GetMapping("/creative-guide")
    public String creativeGuide() {
        return "creative-guide";
    }

    /**
     * 创作引导独立页面（继续指定会话）
     */
    @GetMapping("/creative-guide/{sessionId}")
    public String creativeGuideWithSession(@PathVariable String sessionId, Model model) {
        model.addAttribute("sessionId", sessionId);
        return "creative-guide";
    }
}
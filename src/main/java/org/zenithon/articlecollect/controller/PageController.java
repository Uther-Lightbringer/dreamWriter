package org.zenithon.articlecollect.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.zenithon.articlecollect.entity.Novel;
import org.zenithon.articlecollect.service.NovelService;

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
        model.addAttribute("novel", novel);
        model.addAttribute("novelId", novelId);
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
}
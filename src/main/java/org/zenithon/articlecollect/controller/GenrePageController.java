package org.zenithon.articlecollect.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 体裁页面控制器
 */
@Controller
public class GenrePageController {

    /**
     * 体裁选择页面
     */
    @GetMapping("/genre-select")
    public String genreSelect() {
        return "genre-select";
    }

    /**
     * 体裁引导页面
     */
    @GetMapping("/genre-guide/{genreType}")
    public String genreGuide(@PathVariable String genreType, Model model) {
        String genreName;
        switch (genreType) {
            case "novel":
                genreName = "小说";
                break;
            case "script":
                genreName = "剧本";
                break;
            case "visual":
                genreName = "视觉叙事";
                break;
            case "essay":
                genreName = "散文";
                break;
            default:
                genreName = "创作";
        }
        model.addAttribute("genreType", genreType);
        model.addAttribute("genreName", genreName);
        return "genre-guide";
    }
}

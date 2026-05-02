package org.zenithon.articlecollect.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.zenithon.articlecollect.entity.Essay;
import org.zenithon.articlecollect.entity.EssayParagraph;
import org.zenithon.articlecollect.repository.EssayRepository;
import org.zenithon.articlecollect.repository.EssayParagraphRepository;

import java.util.List;

@Controller
public class EssayPageController {

    @Autowired
    private EssayRepository essayRepository;

    @Autowired
    private EssayParagraphRepository essayParagraphRepository;

    @GetMapping("/essays")
    public String essayList(Model model) {
        List<Essay> essays = essayRepository.findAllByOrderByCreateTimeDesc();
        model.addAttribute("essays", essays);
        return "essays";
    }

    @GetMapping("/essay/{id}")
    public String essayDetail(@PathVariable Long id, Model model) {
        Essay essay = essayRepository.findById(id).orElse(null);
        if (essay == null) return "redirect:/essays";

        List<EssayParagraph> paragraphs = essayParagraphRepository.findByEssayIdOrderByParagraphNumberAsc(id);

        model.addAttribute("essay", essay);
        model.addAttribute("paragraphs", paragraphs);
        return "essay-detail";
    }
}

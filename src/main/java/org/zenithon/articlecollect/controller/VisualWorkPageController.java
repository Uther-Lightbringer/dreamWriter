package org.zenithon.articlecollect.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.zenithon.articlecollect.entity.VisualWork;
import org.zenithon.articlecollect.entity.VisualPanel;
import org.zenithon.articlecollect.repository.VisualWorkRepository;
import org.zenithon.articlecollect.repository.VisualPanelRepository;

import java.util.List;

@Controller
public class VisualWorkPageController {

    @Autowired
    private VisualWorkRepository visualWorkRepository;

    @Autowired
    private VisualPanelRepository visualPanelRepository;

    @GetMapping("/visual-works")
    public String workList(Model model) {
        List<VisualWork> works = visualWorkRepository.findAllByOrderByCreateTimeDesc();
        model.addAttribute("works", works);
        return "visual-works";
    }

    @GetMapping("/visual/{id}")
    public String workDetail(@PathVariable Long id, Model model) {
        VisualWork work = visualWorkRepository.findById(id).orElse(null);
        if (work == null) return "redirect:/visual-works";

        List<VisualPanel> panels = visualPanelRepository.findByWorkIdOrderByPanelNumberAsc(id);

        model.addAttribute("work", work);
        model.addAttribute("panels", panels);
        return "visual-detail";
    }

    @GetMapping("/visual/{id}/panel/{panelId}")
    public String panelDetail(@PathVariable Long id, @PathVariable Long panelId, Model model) {
        VisualWork work = visualWorkRepository.findById(id).orElse(null);
        VisualPanel panel = visualPanelRepository.findById(panelId).orElse(null);

        if (work == null || panel == null) return "redirect:/visual-works";

        List<VisualPanel> allPanels = visualPanelRepository.findByWorkIdOrderByPanelNumberAsc(id);
        int currentIndex = allPanels.indexOf(panel);

        model.addAttribute("work", work);
        model.addAttribute("panel", panel);
        model.addAttribute("allPanels", allPanels);
        model.addAttribute("currentIndex", currentIndex);
        model.addAttribute("prevPanel", currentIndex > 0 ? allPanels.get(currentIndex - 1) : null);
        model.addAttribute("nextPanel", currentIndex < allPanels.size() - 1 ? allPanels.get(currentIndex + 1) : null);

        return "visual-detail";
    }
}

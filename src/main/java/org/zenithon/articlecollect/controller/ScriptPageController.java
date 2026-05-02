package org.zenithon.articlecollect.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.zenithon.articlecollect.entity.Script;
import org.zenithon.articlecollect.entity.ScriptScene;
import org.zenithon.articlecollect.repository.ScriptRepository;
import org.zenithon.articlecollect.repository.ScriptSceneRepository;

import java.util.List;

@Controller
public class ScriptPageController {

    @Autowired
    private ScriptRepository scriptRepository;

    @Autowired
    private ScriptSceneRepository scriptSceneRepository;

    @GetMapping("/scripts")
    public String scriptList(Model model) {
        List<Script> scripts = scriptRepository.findAllByOrderByCreateTimeDesc();
        model.addAttribute("scripts", scripts);
        return "scripts";
    }

    @GetMapping("/script/{id}")
    public String scriptDetail(@PathVariable Long id, Model model) {
        Script script = scriptRepository.findById(id).orElse(null);
        if (script == null) return "redirect:/scripts";

        List<ScriptScene> scenes = scriptSceneRepository.findByScriptIdOrderBySceneNumberAsc(id);

        model.addAttribute("script", script);
        model.addAttribute("scenes", scenes);
        return "script-detail";
    }

    @GetMapping("/script/{id}/scene/{sceneId}")
    public String sceneDetail(@PathVariable Long id, @PathVariable Long sceneId, Model model) {
        Script script = scriptRepository.findById(id).orElse(null);
        ScriptScene scene = scriptSceneRepository.findById(sceneId).orElse(null);

        if (script == null || scene == null) return "redirect:/scripts";

        List<ScriptScene> allScenes = scriptSceneRepository.findByScriptIdOrderBySceneNumberAsc(id);
        int currentIndex = allScenes.indexOf(scene);

        model.addAttribute("script", script);
        model.addAttribute("scene", scene);
        model.addAttribute("allScenes", allScenes);
        model.addAttribute("currentIndex", currentIndex);
        model.addAttribute("prevScene", currentIndex > 0 ? allScenes.get(currentIndex - 1) : null);
        model.addAttribute("nextScene", currentIndex < allScenes.size() - 1 ? allScenes.get(currentIndex + 1) : null);

        return "script-detail";
    }
}

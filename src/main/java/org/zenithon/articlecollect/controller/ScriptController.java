package org.zenithon.articlecollect.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zenithon.articlecollect.entity.Script;
import org.zenithon.articlecollect.entity.ScriptScene;
import org.zenithon.articlecollect.repository.ScriptRepository;
import org.zenithon.articlecollect.repository.ScriptSceneRepository;

import java.util.List;

@RestController
@RequestMapping("/api/scripts")
public class ScriptController {

    @Autowired
    private ScriptRepository scriptRepository;

    @Autowired
    private ScriptSceneRepository scriptSceneRepository;

    @GetMapping
    public ResponseEntity<List<Script>> getAllScripts() {
        return ResponseEntity.ok(scriptRepository.findAllByOrderByCreateTimeDesc());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Script> getScript(@PathVariable Long id) {
        return scriptRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/scenes")
    public ResponseEntity<List<ScriptScene>> getScenes(@PathVariable Long id) {
        return ResponseEntity.ok(scriptSceneRepository.findByScriptIdOrderBySceneNumberAsc(id));
    }

    @GetMapping("/scenes/{sceneId}")
    public ResponseEntity<ScriptScene> getScene(@PathVariable Long sceneId) {
        return scriptSceneRepository.findById(sceneId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScript(@PathVariable Long id) {
        scriptSceneRepository.deleteByScriptId(id);
        scriptRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}

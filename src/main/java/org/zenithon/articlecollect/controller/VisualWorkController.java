package org.zenithon.articlecollect.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zenithon.articlecollect.entity.VisualWork;
import org.zenithon.articlecollect.entity.VisualPanel;
import org.zenithon.articlecollect.repository.VisualWorkRepository;
import org.zenithon.articlecollect.repository.VisualPanelRepository;

import java.util.List;

@RestController
@RequestMapping("/api/visual-works")
public class VisualWorkController {

    @Autowired
    private VisualWorkRepository visualWorkRepository;

    @Autowired
    private VisualPanelRepository visualPanelRepository;

    @GetMapping
    public ResponseEntity<List<VisualWork>> getAllWorks() {
        return ResponseEntity.ok(visualWorkRepository.findAllByOrderByCreateTimeDesc());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VisualWork> getWork(@PathVariable Long id) {
        return visualWorkRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/panels")
    public ResponseEntity<List<VisualPanel>> getPanels(@PathVariable Long id) {
        return ResponseEntity.ok(visualPanelRepository.findByWorkIdOrderByPanelNumberAsc(id));
    }

    @GetMapping("/panels/{panelId}")
    public ResponseEntity<VisualPanel> getPanel(@PathVariable Long panelId) {
        return visualPanelRepository.findById(panelId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWork(@PathVariable Long id) {
        visualPanelRepository.deleteByWorkId(id);
        visualWorkRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}

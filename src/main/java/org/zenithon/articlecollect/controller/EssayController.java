package org.zenithon.articlecollect.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zenithon.articlecollect.entity.Essay;
import org.zenithon.articlecollect.entity.EssayParagraph;
import org.zenithon.articlecollect.repository.EssayRepository;
import org.zenithon.articlecollect.repository.EssayParagraphRepository;

import java.util.List;

@RestController
@RequestMapping("/api/essays")
public class EssayController {

    @Autowired
    private EssayRepository essayRepository;

    @Autowired
    private EssayParagraphRepository essayParagraphRepository;

    @GetMapping
    public ResponseEntity<List<Essay>> getAllEssays() {
        return ResponseEntity.ok(essayRepository.findAllByOrderByCreateTimeDesc());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Essay> getEssay(@PathVariable Long id) {
        return essayRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/paragraphs")
    public ResponseEntity<List<EssayParagraph>> getParagraphs(@PathVariable Long id) {
        return ResponseEntity.ok(essayParagraphRepository.findByEssayIdOrderByParagraphNumberAsc(id));
    }

    @GetMapping("/paragraphs/{paragraphId}")
    public ResponseEntity<EssayParagraph> getParagraph(@PathVariable Long paragraphId) {
        return essayParagraphRepository.findById(paragraphId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEssay(@PathVariable Long id) {
        essayParagraphRepository.deleteByEssayId(id);
        essayRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}

package org.zenithon.articlecollect.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.zenithon.articlecollect.entity.GenreSession;
import org.zenithon.articlecollect.service.genre.GenreService;
import org.zenithon.articlecollect.service.genre.GenreServiceFactory;

import java.util.List;
import java.util.Map;

/**
 * 体裁API控制器
 */
@RestController
@RequestMapping("/api/genres")
public class GenreController {

    @Autowired
    private GenreServiceFactory genreServiceFactory;

    /**
     * 获取所有可用体裁
     */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAvailableGenres() {
        return ResponseEntity.ok(genreServiceFactory.getAvailableGenres());
    }

    /**
     * 获取体裁会话列表
     */
    @GetMapping("/{genreType}/sessions")
    public ResponseEntity<List<GenreSession>> getSessions(@PathVariable String genreType) {
        GenreService service = genreServiceFactory.getGenreService(genreType);
        return ResponseEntity.ok(service.getAllSessions());
    }

    /**
     * 创建体裁会话
     */
    @PostMapping("/{genreType}/sessions")
    public ResponseEntity<GenreSession> createSession(
            @PathVariable String genreType,
            @RequestParam(required = false) String title) {
        GenreService service = genreServiceFactory.getGenreService(genreType);
        return ResponseEntity.ok(service.createSession(title));
    }

    /**
     * 获取会话详情
     */
    @GetMapping("/{genreType}/sessions/{sessionId}")
    public ResponseEntity<GenreSession> getSession(
            @PathVariable String genreType,
            @PathVariable String sessionId) {
        GenreService service = genreServiceFactory.getGenreService(genreType);
        return ResponseEntity.ok(service.getSession(sessionId));
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/{genreType}/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(
            @PathVariable String genreType,
            @PathVariable String sessionId) {
        GenreService service = genreServiceFactory.getGenreService(genreType);
        service.deleteSession(sessionId);
        return ResponseEntity.ok().build();
    }

    /**
     * 发送消息（SSE流式）
     */
    @PostMapping("/{genreType}/sessions/{sessionId}/chat")
    public SseEmitter chat(
            @PathVariable String genreType,
            @PathVariable String sessionId,
            @RequestBody Map<String, String> request) {
        GenreService service = genreServiceFactory.getGenreService(genreType);
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时
        String content = request.get("content");
        service.chat(sessionId, content, emitter);
        return emitter;
    }
}

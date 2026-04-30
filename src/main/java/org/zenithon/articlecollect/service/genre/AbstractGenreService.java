package org.zenithon.articlecollect.service.genre;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.zenithon.articlecollect.entity.GenreSession;
import org.zenithon.articlecollect.repository.GenreSessionRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 体裁服务抽象基类
 * 提供通用的会话管理和工具方法
 */
public abstract class AbstractGenreService implements GenreService {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    protected GenreSessionRepository sessionRepository;

    @Autowired
    protected ObjectMapper objectMapper;

    @Override
    public GenreSession createSession(String title) {
        String sessionId = "genre_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        GenreSession session = new GenreSession();
        session.setSessionId(sessionId);
        session.setGenreType(getGenreType());

        if (title != null && !title.trim().isEmpty()) {
            session.setTitle(title);
        } else {
            session.setTitle(getGenreName() + "会话 " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd HH:mm")));
        }

        // 初始化消息历史
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", getSystemPrompt()));
        session.setMessages(toJson(messages));

        // 初始化参数
        session.setExtractedParams(toJson(getInitialParams()));

        return sessionRepository.save(session);
    }

    @Override
    public List<GenreSession> getAllSessions() {
        return sessionRepository.findByGenreTypeOrderByUpdateTimeDesc(getGenreType());
    }

    @Override
    public GenreSession getSession(String sessionId) {
        return sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("会话不存在: " + sessionId));
    }

    @Override
    public void deleteSession(String sessionId) {
        sessionRepository.deleteBySessionId(sessionId);
    }

    /**
     * 获取初始参数（由子类实现）
     */
    protected abstract Map<String, Object> getInitialParams();

    /**
     * JSON序列化
     */
    protected String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("JSON序列化失败: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * JSON反序列化
     */
    protected <T> T fromJson(String json, TypeReference<T> typeRef) {
        try {
            if (json == null || json.isEmpty()) {
                return objectMapper.readValue("{}", typeRef);
            }
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            logger.error("JSON反序列化失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析消息历史
     */
    protected List<Map<String, Object>> parseMessages(String messagesJson) {
        return fromJson(messagesJson, new TypeReference<List<Map<String, Object>>>() {});
    }
}

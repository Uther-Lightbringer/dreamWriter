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

    private static final int SUMMARY_THRESHOLD = 15;
    private static final int KEEP_RECENT_TURNS = 5;

    @Override
    public Map<String, Object> getSessionStats(String sessionId) {
        GenreSession session = getSession(sessionId);
        List<Map<String, Object>> messages = parseMessages(session.getMessages());

        int messageCount = messages.size();
        int userTurns = countUserTurns(messages);

        // 估算 token 数（粗略：每 4 个字符约 1 token）
        int estimatedTokens = 0;
        for (Map<String, Object> msg : messages) {
            String content = (String) msg.get("content");
            if (content != null) {
                estimatedTokens += content.length() / 4;
            }
        }

        int thresholdPercentage = (userTurns * 100) / SUMMARY_THRESHOLD;

        return Map.of(
            "messageCount", messageCount,
            "userTurns", userTurns,
            "estimatedTokens", estimatedTokens,
            "thresholdPercentage", Math.min(thresholdPercentage, 100),
            "needsCompression", userTurns > SUMMARY_THRESHOLD
        );
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Map<String, Object> compressSession(String sessionId) {
        GenreSession session = getSession(sessionId);
        List<Map<String, Object>> messages = parseMessages(session.getMessages());

        int messagesBefore = messages.size();
        int userTurnsBefore = countUserTurns(messages);

        if (messages.size() <= (KEEP_RECENT_TURNS * 2 + 2)) {
            return Map.of(
                "success", false,
                "message", "对话较短，无需压缩",
                "messageCount", messages.size(),
                "userTurns", userTurnsBefore
            );
        }

        // 保留系统提示词
        Map<String, Object> systemMessage = messages.get(0);

        // 保留最近的消息
        int keepMessageCount = KEEP_RECENT_TURNS * 2;
        List<Map<String, Object>> recentMessages = new ArrayList<>(
            messages.subList(messages.size() - keepMessageCount, messages.size())
        );

        // 构建状态摘要
        Map<String, Object> params = new HashMap<>();
        String paramsJson = session.getExtractedParams();
        if (paramsJson != null && !paramsJson.isEmpty()) {
            Map<String, Object> parsed = fromJson(paramsJson, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            if (parsed != null) params = parsed;
        }

        StringBuilder summary = new StringBuilder();
        summary.append("[").append(getGenreName()).append("创作状态摘要]\n\n");

        // 参数摘要
        if (!params.isEmpty()) {
            summary.append("## 已确认参数\n");
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                Object value = entry.getValue();
                if (value != null && !value.toString().isEmpty()) {
                    summary.append("- ").append(entry.getKey()).append("：").append(value).append("\n");
                }
            }
            summary.append("\n");
        }

        // 对话摘要
        summary.append("## 对话摘要\n");
        summary.append("此前共进行了 ").append(userTurnsBefore).append(" 轮对话。\n");

        Map<String, Object> stateSummary = Map.of("role", "system", "content", summary.toString());

        // 替换消息历史
        messages.clear();
        messages.add(systemMessage);
        messages.add(stateSummary);
        messages.addAll(recentMessages);

        session.setMessages(toJson(messages));
        sessionRepository.save(session);

        int messagesAfter = messages.size();
        int userTurnsAfter = countUserTurns(messages);
        int turnsCompressed = userTurnsBefore - userTurnsAfter;

        return Map.of(
            "success", true,
            "message", "压缩完成",
            "messagesBefore", messagesBefore,
            "messagesAfter", messagesAfter,
            "userTurnsBefore", userTurnsBefore,
            "userTurnsAfter", userTurnsAfter,
            "turnsCompressed", turnsCompressed
        );
    }

    @Override
    public void updateSessionTitle(String sessionId, String title) {
        GenreSession session = getSession(sessionId);
        session.setTitle(title);
        sessionRepository.save(session);
    }

    private int countUserTurns(List<Map<String, Object>> messages) {
        int count = 0;
        for (Map<String, Object> msg : messages) {
            if ("user".equals(msg.get("role"))) {
                count++;
            }
        }
        return count;
    }

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

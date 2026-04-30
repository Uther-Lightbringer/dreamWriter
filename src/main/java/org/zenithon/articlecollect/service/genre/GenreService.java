package org.zenithon.articlecollect.service.genre;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.zenithon.articlecollect.entity.GenreSession;
import java.util.List;
import java.util.Map;

/**
 * 体裁服务接口
 * 定义所有体裁服务必须实现的方法
 */
public interface GenreService {

    /**
     * 获取体裁类型标识
     */
    String getGenreType();

    /**
     * 获取体裁名称（中文）
     */
    String getGenreName();

    /**
     * 获取系统提示词
     */
    String getSystemPrompt();

    /**
     * 获取工具列表
     */
    List<Map<String, Object>> getTools();

    /**
     * 创建新会话
     */
    GenreSession createSession(String title);

    /**
     * 发送消息并获取流式响应
     */
    void chat(String sessionId, String content, SseEmitter emitter);

    /**
     * 获取所有会话列表
     */
    List<GenreSession> getAllSessions();

    /**
     * 获取会话详情
     */
    GenreSession getSession(String sessionId);

    /**
     * 删除会话
     */
    void deleteSession(String sessionId);
}

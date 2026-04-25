package org.zenithon.articlecollect.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 创作引导会话实体类
 * 用于存储用户与 AI 的创作引导对话记录
 */
@Entity
@Table(name = "creative_sessions")
public class CreativeSession {

    /**
     * 会话状态枚举
     */
    public enum SessionStatus {
        ACTIVE,     // 活跃
        ARCHIVED    // 已归档
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true, length = 64)
    private String sessionId;          // 唯一标识

    @Column(length = 255)
    private String title;              // 会话标题（AI 生成或用户命名）

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SessionStatus status = SessionStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String messages;           // 完整消息历史（JSON 格式，用于 DeepSeek API）

    @Column(name = "extracted_params", columnDefinition = "TEXT")
    private String extractedParams;    // 当前提取的参数（JSON 格式）

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    public CreativeSession() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    public CreativeSession(String sessionId) {
        this.sessionId = sessionId;
        this.status = SessionStatus.ACTIVE;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    // ========== Getters and Setters ==========

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.updateTime = LocalDateTime.now();
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
        this.updateTime = LocalDateTime.now();
    }

    public String getMessages() {
        return messages;
    }

    public void setMessages(String messages) {
        this.messages = messages;
        this.updateTime = LocalDateTime.now();
    }

    public String getExtractedParams() {
        return extractedParams;
    }

    public void setExtractedParams(String extractedParams) {
        this.extractedParams = extractedParams;
        this.updateTime = LocalDateTime.now();
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "CreativeSession{" +
                "id=" + id +
                ", sessionId='" + sessionId + '\'' +
                ", title='" + title + '\'' +
                ", status=" + status +
                '}';
    }
}

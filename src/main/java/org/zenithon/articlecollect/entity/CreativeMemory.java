package org.zenithon.articlecollect.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 跨会话记忆实体类
 * 用于存储用户的偏好和重要信息
 *
 * 支持两种记忆模式：
 * 1. 全局记忆（sessionId = null）：跨会话共享的用户偏好
 * 2. 会话记忆（sessionId != null）：特定会话的独立记忆，优先级高于全局记忆
 */
@Entity
@Table(name = "creative_memories", indexes = {
    @Index(name = "idx_session_key", columnList = "session_id, `key`")
})
public class CreativeMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, name = "`key`")
    private String key;              // 记忆类型，如 "preferred_style", "preferred_genre"

    @Column(nullable = false, columnDefinition = "TEXT", name = "`value`")
    private String value;            // 记忆内容

    @Column(name = "session_id", length = 64)
    private String sessionId;        // 所属会话 ID，null 表示全局记忆

    @Column(name = "source_session_id", length = 64)
    private String sourceSessionId;  // 来源会话 ID（兼容旧数据）

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    public CreativeMemory() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 创建全局记忆
     */
    public CreativeMemory(String key, String value) {
        this.key = key;
        this.value = value;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 创建会话特定记忆
     */
    public CreativeMemory(String key, String value, String sessionId, boolean isSessionMemory) {
        this.key = key;
        this.value = value;
        this.sessionId = isSessionMemory ? sessionId : null;
        this.sourceSessionId = sessionId;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 兼容旧构造函数
     */
    public CreativeMemory(String key, String value, String sourceSessionId) {
        this.key = key;
        this.value = value;
        this.sourceSessionId = sourceSessionId;
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

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
        this.updateTime = LocalDateTime.now();
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
        this.updateTime = LocalDateTime.now();
    }

    public String getSourceSessionId() {
        return sourceSessionId;
    }

    public void setSourceSessionId(String sourceSessionId) {
        this.sourceSessionId = sourceSessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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
        return "CreativeMemory{" +
                "id=" + id +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", sourceSessionId='" + sourceSessionId + '\'' +
                '}';
    }
}

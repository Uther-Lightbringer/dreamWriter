package org.zenithon.articlecollect.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 跨会话记忆实体类
 * 用于存储用户的偏好和重要信息，跨会话保持
 */
@Entity
@Table(name = "creative_memories")
public class CreativeMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, name = "`key`")
    private String key;              // 记忆类型，如 "preferred_style", "preferred_genre"

    @Column(nullable = false, columnDefinition = "TEXT", name = "`value`")
    private String value;            // 记忆内容

    @Column(name = "source_session_id", length = 64)
    private String sourceSessionId;  // 来源会话 ID

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    public CreativeMemory() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    public CreativeMemory(String key, String value) {
        this.key = key;
        this.value = value;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

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
                ", sourceSessionId='" + sourceSessionId + '\'' +
                '}';
    }
}

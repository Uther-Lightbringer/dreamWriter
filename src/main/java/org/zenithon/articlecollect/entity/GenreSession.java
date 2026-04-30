package org.zenithon.articlecollect.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 体裁会话实体
 * 存储不同体裁的创作会话数据
 */
@Entity
@Table(name = "genre_sessions")
public class GenreSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", unique = true, nullable = false)
    private String sessionId;

    @Column(name = "genre_type", nullable = false)
    private String genreType;

    @Column(name = "title")
    private String title;

    @Column(name = "status")
    private String status = "ACTIVE";

    @Column(name = "context_data", columnDefinition = "TEXT")
    private String contextData;

    @Column(name = "extracted_params", columnDefinition = "TEXT")
    private String extractedParams;

    @Column(name = "messages", columnDefinition = "TEXT")
    private String messages;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getGenreType() { return genreType; }
    public void setGenreType(String genreType) { this.genreType = genreType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getContextData() { return contextData; }
    public void setContextData(String contextData) { this.contextData = contextData; }

    public String getExtractedParams() { return extractedParams; }
    public void setExtractedParams(String extractedParams) { this.extractedParams = extractedParams; }

    public String getMessages() { return messages; }
    public void setMessages(String messages) { this.messages = messages; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}

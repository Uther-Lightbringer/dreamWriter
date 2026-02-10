package org.zenithon.articlecollect.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 章节标签实体类
 * 用于存储章节内容分析后的标签信息
 */
@Entity
@Table(name = "chapter_tags")
public class ChapterTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "chapter_id")
    private Long chapterId;
    
    @Column(name = "tag_type", nullable = false, length = 50)
    private String tagType; // 标签类型：punishment(惩罚), reward(奖励), training(训练), discipline(调教)
    
    @Column(name = "tag_value", nullable = false, length = 100)
    private String tagValue; // 标签值
    
    @Column(name = "detected_keywords", columnDefinition = "TEXT")
    private String detectedKeywords; // 检测到的关键词
    
    @Column(name = "create_time")
    private LocalDateTime createTime;
    
    // 构造函数
    public ChapterTag() {
        this.createTime = LocalDateTime.now();
    }
    
    public ChapterTag(Long chapterId, String tagType, String tagValue, String detectedKeywords) {
        this.chapterId = chapterId;
        this.tagType = tagType;
        this.tagValue = tagValue;
        this.detectedKeywords = detectedKeywords;
        this.createTime = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getChapterId() {
        return chapterId;
    }
    
    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }
    
    public String getTagType() {
        return tagType;
    }
    
    public void setTagType(String tagType) {
        this.tagType = tagType;
    }
    
    public String getTagValue() {
        return tagValue;
    }
    
    public void setTagValue(String tagValue) {
        this.tagValue = tagValue;
    }
    
    public String getDetectedKeywords() {
        return detectedKeywords;
    }
    
    public void setDetectedKeywords(String detectedKeywords) {
        this.detectedKeywords = detectedKeywords;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    @Override
    public String toString() {
        return "ChapterTag{" +
                "id=" + id +
                ", chapterId=" + chapterId +
                ", tagType='" + tagType + '\'' +
                ", tagValue='" + tagValue + '\'' +
                ", detectedKeywords='" + detectedKeywords + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}
package org.zenithon.articlecollect.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * AI 图片生成历史记录实体类
 */
@Entity
@Table(name = "ai_image_history")
public class AiImageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "novel_id")
    private Long novelId;

    @Column(name = "novel_title", length = 500)
    private String novelTitle;

    @Column(name = "chapter_id")
    private Long chapterId;

    @Column(name = "chapter_title", length = 500)
    private String chapterTitle;

    public AiImageHistory() {
    }

    public AiImageHistory(String prompt, String imageUrl) {
        this.prompt = prompt;
        this.imageUrl = imageUrl;
        this.createTime = LocalDateTime.now();
    }

    public AiImageHistory(String prompt, String imageUrl, Long novelId, String novelTitle, Long chapterId, String chapterTitle) {
        this.prompt = prompt;
        this.imageUrl = imageUrl;
        this.novelId = novelId;
        this.novelTitle = novelTitle;
        this.chapterId = chapterId;
        this.chapterTitle = chapterTitle;
        this.createTime = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Long getNovelId() {
        return novelId;
    }

    public void setNovelId(Long novelId) {
        this.novelId = novelId;
    }

    public String getNovelTitle() {
        return novelTitle;
    }

    public void setNovelTitle(String novelTitle) {
        this.novelTitle = novelTitle;
    }

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public String getChapterTitle() {
        return chapterTitle;
    }

    public void setChapterTitle(String chapterTitle) {
        this.chapterTitle = chapterTitle;
    }
}

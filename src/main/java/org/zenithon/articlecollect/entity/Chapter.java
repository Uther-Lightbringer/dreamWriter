package org.zenithon.articlecollect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 章节实体类
 */
@Entity
@Table(name = "chapters")
public class Chapter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "novel_id")
    private Long novelId;
    
    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "chapter_number")
    private Integer chapterNumber;
    
    @Column(name = "create_time")
    private LocalDateTime createTime;
    
    @Column(name = "update_time")
    private LocalDateTime updateTime;
    
    // 多对一关系：多个章节对应一个小说
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "novel_id", insertable = false, updatable = false)
    @JsonIgnore
    private Novel novel;
    
    // 章节插图路径
    @Column(name = "chapter_image", length = 500)
    private String chapterImage;
    
    // 用于Thymeleaf模板的格式化时间字符串
    @Transient
    private String formattedCreateTime;

    public Chapter() {
    }

    public Chapter(Long novelId, String title, String content, Integer chapterNumber) {
        this.novelId = novelId;
        this.title = title;
        this.content = content;
        this.chapterNumber = chapterNumber;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        this.formattedCreateTime = formatTime(this.createTime);
    }

    public Chapter(Long id, Long novelId, String title, String content, Integer chapterNumber) {
        this.id = id;
        this.novelId = novelId;
        this.title = title;
        this.content = content;
        this.chapterNumber = chapterNumber;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        this.formattedCreateTime = formatTime(this.createTime);
    }

    // getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNovelId() {
        return novelId;
    }

    public void setNovelId(Long novelId) {
        this.novelId = novelId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.updateTime = LocalDateTime.now();
        this.formattedCreateTime = formatTime(this.updateTime);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.updateTime = LocalDateTime.now();
        this.formattedCreateTime = formatTime(this.updateTime);
    }

    public Integer getChapterNumber() {
        return chapterNumber;
    }

    public void setChapterNumber(Integer chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
        this.formattedCreateTime = formatTime(createTime);
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
        this.formattedCreateTime = formatTime(updateTime);
    }
    
    public String getChapterImage() {
        return chapterImage;
    }
    
    public void setChapterImage(String chapterImage) {
        this.chapterImage = chapterImage;
    }
    
    public Novel getNovel() {
        return novel;
    }
    
    public void setNovel(Novel novel) {
        this.novel = novel;
    }
    
    public String getFormattedCreateTime() {
        return formattedCreateTime;
    }
    
    public void setFormattedCreateTime(String formattedCreateTime) {
        this.formattedCreateTime = formattedCreateTime;
    }
    
    // 公共方法：格式化时间供外部使用
    public String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
    
    // 私有方法：格式化时间
    private String formatTime(LocalDateTime dateTime) {
        return formatDateTime(dateTime);
    }

    @Override
    public String toString() {
        return "Chapter{" +
                "id=" + id +
                ", novelId=" + novelId +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", chapterNumber=" + chapterNumber +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", chapterImage='" + getChapterImage() + '\'' +
                '}';
    }
}
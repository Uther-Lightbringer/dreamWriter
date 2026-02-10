package org.zenithon.articlecollect.dto;

import org.zenithon.articlecollect.entity.Chapter;
import org.zenithon.articlecollect.entity.ChapterTag;

import java.util.List;

/**
 * 包含标签信息的章节DTO
 */
public class ChapterWithTags {
    private Chapter chapter;
    private List<ChapterTag> tags;
    
    public ChapterWithTags() {}
    
    public ChapterWithTags(Chapter chapter, List<ChapterTag> tags) {
        this.chapter = chapter;
        this.tags = tags;
    }
    
    // Getters and Setters
    public Chapter getChapter() {
        return chapter;
    }
    
    public void setChapter(Chapter chapter) {
        this.chapter = chapter;
    }
    
    public List<ChapterTag> getTags() {
        return tags;
    }
    
    public void setTags(List<ChapterTag> tags) {
        this.tags = tags;
    }
    
    // 便捷方法
    public Long getId() {
        return chapter != null ? chapter.getId() : null;
    }
    
    public String getTitle() {
        return chapter != null ? chapter.getTitle() : null;
    }
    
    public String getContent() {
        return chapter != null ? chapter.getContent() : null;
    }
    
    public Integer getChapterNumber() {
        return chapter != null ? chapter.getChapterNumber() : null;
    }
    
    public String getFormattedCreateTime() {
        return chapter != null ? chapter.getFormattedCreateTime() : null;
    }
    
    public String getChapterImage() {
        return chapter != null ? chapter.getChapterImage() : null;
    }
    
    @Override
    public String toString() {
        return "ChapterWithTags{" +
                "chapter=" + chapter +
                ", tags=" + tags +
                '}';
    }
}
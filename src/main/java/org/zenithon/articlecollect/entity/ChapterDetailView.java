package org.zenithon.articlecollect.entity;

import java.time.LocalDateTime;

/**
 * 小说章节详情视图实体
 * 用于章节详情页面显示，包含导航信息
 */
public class ChapterDetailView {
    private Chapter chapter;
    private Boolean hasNext;
    private Boolean hasPrevious;
    private Long nextChapterId;
    private Long previousChapterId;
    private String novelTitle;
    private Long novelId;

    public ChapterDetailView() {
    }

    public ChapterDetailView(Chapter chapter, Boolean hasNext, Boolean hasPrevious, 
                           Long nextChapterId, Long previousChapterId, String novelTitle, Long novelId) {
        this.chapter = chapter;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
        this.nextChapterId = nextChapterId;
        this.previousChapterId = previousChapterId;
        this.novelTitle = novelTitle;
        this.novelId = novelId;
    }

    // getters and setters
    public Chapter getChapter() {
        return chapter;
    }

    public void setChapter(Chapter chapter) {
        this.chapter = chapter;
    }

    public Boolean getHasNext() {
        return hasNext;
    }

    public void setHasNext(Boolean hasNext) {
        this.hasNext = hasNext;
    }

    public Boolean getHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(Boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }

    public Long getNextChapterId() {
        return nextChapterId;
    }

    public void setNextChapterId(Long nextChapterId) {
        this.nextChapterId = nextChapterId;
    }

    public Long getPreviousChapterId() {
        return previousChapterId;
    }

    public void setPreviousChapterId(Long previousChapterId) {
        this.previousChapterId = previousChapterId;
    }

    public String getNovelTitle() {
        return novelTitle;
    }

    public void setNovelTitle(String novelTitle) {
        this.novelTitle = novelTitle;
    }

    public Long getNovelId() {
        return novelId;
    }

    public void setNovelId(Long novelId) {
        this.novelId = novelId;
    }
}
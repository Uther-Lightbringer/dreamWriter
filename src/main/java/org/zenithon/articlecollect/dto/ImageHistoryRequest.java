package org.zenithon.articlecollect.dto;

/**
 * 图片历史记录请求 DTO
 */
public class ImageHistoryRequest {

    private String prompt;
    private String imageUrl;
    private Long novelId;
    private String novelTitle;
    private Long chapterId;
    private String chapterTitle;
    private String novelContent;

    public ImageHistoryRequest() {
    }

    public ImageHistoryRequest(String prompt, String imageUrl) {
        this.prompt = prompt;
        this.imageUrl = imageUrl;
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

    public String getNovelContent() {
        return novelContent;
    }

    public void setNovelContent(String novelContent) {
        this.novelContent = novelContent;
    }
}

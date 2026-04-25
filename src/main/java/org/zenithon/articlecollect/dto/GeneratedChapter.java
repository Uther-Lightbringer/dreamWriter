package org.zenithon.articlecollect.dto;

/**
 * 生成的章节数据DTO
 */
public class GeneratedChapter {

    private String section;         // 章节标题
    private String description;     // 章节概述
    private String content;         // 章节内容
    private Integer index;          // 章节序号

    public GeneratedChapter() {
    }

    public GeneratedChapter(String section, String description) {
        this.section = section;
        this.description = description;
    }

    // ========== Getters and Setters ==========

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return "GeneratedChapter{" +
                "section='" + section + '\'' +
                ", description='" + (description != null ? description.substring(0, Math.min(50, description.length())) + "..." : "null") + '\'' +
                ", index=" + index +
                '}';
    }
}

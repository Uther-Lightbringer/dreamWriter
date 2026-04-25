package org.zenithon.articlecollect.dto;

import java.util.List;

/**
 * 生成的大纲结果DTO
 */
public class GeneratedOutline {

    private String outline;                     // 整体大纲
    private List<ChapterInfo> chapters;         // 章节列表

    public GeneratedOutline() {
    }

    public GeneratedOutline(String outline, List<ChapterInfo> chapters) {
        this.outline = outline;
        this.chapters = chapters;
    }

    // ========== Getters and Setters ==========

    public String getOutline() {
        return outline;
    }

    public void setOutline(String outline) {
        this.outline = outline;
    }

    public List<ChapterInfo> getChapters() {
        return chapters;
    }

    public void setChapters(List<ChapterInfo> chapters) {
        this.chapters = chapters;
    }

    /**
     * 章节信息内部类
     */
    public static class ChapterInfo {
        private String section;       // 章节标题
        private String description;   // 章节概述

        public ChapterInfo() {
        }

        public ChapterInfo(String section, String description) {
            this.section = section;
            this.description = description;
        }

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

        @Override
        public String toString() {
            return "ChapterInfo{" +
                    "section='" + section + '\'' +
                    ", description='" + (description != null ? description.substring(0, Math.min(30, description.length())) + "..." : "null") + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "GeneratedOutline{" +
                "outline='" + (outline != null ? outline.substring(0, Math.min(50, outline.length())) + "..." : "null") + '\'' +
                ", chapters=" + (chapters != null ? chapters.size() : 0) + " chapters" +
                '}';
    }
}

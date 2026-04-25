package org.zenithon.articlecollect.dto;

/**
 * 小说生成器请求参数DTO
 */
public class NovelGeneratorRequest {

    /**
     * 章节数量
     */
    private Integer chapterCount = 8;

    /**
     * 关键词/故事大纲
     */
    private String keyword;

    /**
     * 文学体裁
     */
    private String genre = "记叙文";

    /**
     * 扩写需求
     */
    private String requires;

    /**
     * 是否生成新世界观
     */
    private Boolean newWorld = false;

    /**
     * 角色数量
     */
    private Integer roleCount = 3;

    /**
     * 玩法类型
     */
    private String gameplay = "BDSM";

    /**
     * 工具类型
     */
    private String tools = "BDSM";

    /**
     * 叙述视角
     */
    private String pointOfView = "第三人称全知视角";

    /**
     * 主角名称
     */
    private String protagonist;

    /**
     * 语言风格
     */
    private String languageStyle = "直白、露骨的性虐。性爱描写";

    /**
     * 每章字数要求
     */
    private Integer wordsPerChapter = 3000;

    /**
     * AI模型选择
     * deepseek-v4-pro 或 deepseek-v4-flash
     */
    private String aiModel = "deepseek-v4-pro";

    // ========== Getters and Setters ==========

    public Integer getChapterCount() {
        return chapterCount;
    }

    public void setChapterCount(Integer chapterCount) {
        this.chapterCount = chapterCount;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getRequires() {
        return requires;
    }

    public void setRequires(String requires) {
        this.requires = requires;
    }

    public Boolean getNewWorld() {
        return newWorld;
    }

    public void setNewWorld(Boolean newWorld) {
        this.newWorld = newWorld;
    }

    public Integer getRoleCount() {
        return roleCount;
    }

    public void setRoleCount(Integer roleCount) {
        this.roleCount = roleCount;
    }

    public String getGameplay() {
        return gameplay;
    }

    public void setGameplay(String gameplay) {
        this.gameplay = gameplay;
    }

    public String getTools() {
        return tools;
    }

    public void setTools(String tools) {
        this.tools = tools;
    }

    public String getPointOfView() {
        return pointOfView;
    }

    public void setPointOfView(String pointOfView) {
        this.pointOfView = pointOfView;
    }

    public String getProtagonist() {
        return protagonist;
    }

    public void setProtagonist(String protagonist) {
        this.protagonist = protagonist;
    }

    public String getLanguageStyle() {
        return languageStyle;
    }

    public void setLanguageStyle(String languageStyle) {
        this.languageStyle = languageStyle;
    }

    public Integer getWordsPerChapter() {
        return wordsPerChapter;
    }

    public void setWordsPerChapter(Integer wordsPerChapter) {
        this.wordsPerChapter = wordsPerChapter;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }

    /**
     * 验证请求参数
     */
    public boolean isValid() {
        return chapterCount != null && chapterCount > 0 && chapterCount <= 50
                && keyword != null && !keyword.trim().isEmpty()
                && roleCount != null && roleCount > 0 && roleCount <= 20;
    }

    @Override
    public String toString() {
        return "NovelGeneratorRequest{" +
                "chapterCount=" + chapterCount +
                ", keyword='" + (keyword != null ? keyword.substring(0, Math.min(50, keyword.length())) + "..." : "null") + '\'' +
                ", genre='" + genre + '\'' +
                ", newWorld=" + newWorld +
                ", roleCount=" + roleCount +
                ", gameplay='" + gameplay + '\'' +
                ", tools='" + tools + '\'' +
                ", pointOfView='" + pointOfView + '\'' +
                ", protagonist='" + protagonist + '\'' +
                ", languageStyle='" + languageStyle + '\'' +
                '}';
    }
}

package org.zenithon.articlecollect.dto;

import org.zenithon.articlecollect.entity.NovelGeneratorTask;

/**
 * 小说生成器进度响应DTO
 */
public class NovelGeneratorProgress {

    private String taskId;
    private String status;
    private String currentStep;
    private Integer progress;
    private Long resultNovelId;
    private String resultNovelTitle;
    private String errorMessage;
    private Long durationSeconds;
    private String createTime;
    private Integer chapterCount;

    // 生成参数
    private String keyword;
    private String genre;
    private String requires;
    private Boolean newWorld;
    private Integer roleCount;
    private String gameplay;
    private String tools;
    private String pointOfView;
    private String protagonist;
    private String languageStyle;
    private Integer wordsPerChapter;

    public NovelGeneratorProgress() {
    }

    public NovelGeneratorProgress(NovelGeneratorTask task) {
        this.taskId = task.getTaskId();
        this.status = task.getStatus().name();
        this.currentStep = task.getCurrentStep();
        this.progress = task.getProgress();
        this.resultNovelId = task.getResultNovelId();
        this.resultNovelTitle = task.getResultNovelTitle();
        this.errorMessage = task.getErrorMessage();
        this.durationSeconds = task.getDurationSeconds();
        this.createTime = task.getFormattedCreateTime();
        this.chapterCount = task.getChapterCount();

        // 生成参数
        this.keyword = task.getKeyword();
        this.genre = task.getGenre();
        this.requires = task.getRequires();
        this.newWorld = task.getNewWorld();
        this.roleCount = task.getRoleCount();
        this.gameplay = task.getGameplay();
        this.tools = task.getTools();
        this.pointOfView = task.getPointOfView();
        this.protagonist = task.getProtagonist();
        this.languageStyle = task.getLanguageStyle();
        this.wordsPerChapter = task.getWordsPerChapter();
    }

    // ========== Getters and Setters ==========

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public Long getResultNovelId() {
        return resultNovelId;
    }

    public void setResultNovelId(Long resultNovelId) {
        this.resultNovelId = resultNovelId;
    }

    public String getResultNovelTitle() {
        return resultNovelTitle;
    }

    public void setResultNovelTitle(String resultNovelTitle) {
        this.resultNovelTitle = resultNovelTitle;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

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

    /**
     * 是否完成
     */
    public boolean isCompleted() {
        return "COMPLETED".equals(status);
    }

    /**
     * 是否失败
     */
    public boolean isFailed() {
        return "FAILED".equals(status);
    }

    /**
     * 是否运行中
     */
    public boolean isRunning() {
        return "RUNNING".equals(status);
    }

    @Override
    public String toString() {
        return "NovelGeneratorProgress{" +
                "taskId='" + taskId + '\'' +
                ", status='" + status + '\'' +
                ", currentStep='" + currentStep + '\'' +
                ", progress=" + progress +
                ", resultNovelId=" + resultNovelId +
                '}';
    }
}

package org.zenithon.articlecollect.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 小说生成器任务实体类
 * 用于存储生成任务的状态和进度
 */
@Entity
@Table(name = "novel_generator_tasks")
public class NovelGeneratorTask {

    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        PENDING,    // 等待执行
        RUNNING,    // 执行中
        COMPLETED,  // 已完成
        FAILED      // 失败
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false, unique = true, length = 64)
    private String taskId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TaskStatus status = TaskStatus.PENDING;

    @Column(name = "current_step", length = 100)
    private String currentStep;

    @Column(nullable = false)
    private Integer progress = 0;

    // ========== 输入参数 ==========

    @Column(name = "chapter_count")
    private Integer chapterCount;

    @Column(columnDefinition = "TEXT")
    private String keyword;

    @Column(length = 100)
    private String genre;

    @Column(columnDefinition = "TEXT")
    private String requires;

    @Column(name = "new_world")
    private Boolean newWorld;

    @Column(name = "role_count")
    private Integer roleCount;

    @Column(length = 100)
    private String gameplay;

    @Column(length = 100)
    private String tools;

    @Column(name = "point_of_view", length = 50)
    private String pointOfView;

    @Column(length = 255)
    private String protagonist;

    @Column(name = "language_style", length = 100)
    private String languageStyle;

    @Column(name = "words_per_chapter")
    private Integer wordsPerChapter;

    @Column(name = "ai_model", length = 50)
    private String aiModel;

    // ========== 中间结果 ==========

    @Column(name = "generated_tools", columnDefinition = "TEXT")
    private String generatedTools;

    @Column(name = "generated_gameplay", columnDefinition = "TEXT")
    private String generatedGameplay;

    @Column(name = "generated_worldview", columnDefinition = "TEXT")
    private String generatedWorldview;

    @Column(name = "generated_characters", columnDefinition = "TEXT")
    private String generatedCharacters;

    @Column(name = "generated_outline", columnDefinition = "TEXT")
    private String generatedOutline;

    @Column(name = "generated_chapters", columnDefinition = "TEXT")
    private String generatedChapters;

    // ========== 最终结果 ==========

    @Column(name = "result_novel_id")
    private Long resultNovelId;

    @Column(name = "result_novel_title", length = 255)
    private String resultNovelTitle;

    // ========== 错误信息 ==========

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // ========== 时间戳 ==========

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    public NovelGeneratorTask() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    public NovelGeneratorTask(String taskId) {
        this.taskId = taskId;
        this.status = TaskStatus.PENDING;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    // ========== 辅助方法 ==========

    /**
     * 更新进度
     */
    public void updateProgress(String step, int progress) {
        this.currentStep = step;
        this.progress = Math.min(100, Math.max(0, progress));
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 标记为运行中
     */
    public void markAsRunning() {
        this.status = TaskStatus.RUNNING;
        this.startTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 标记为完成
     */
    public void markAsCompleted(Long novelId, String novelTitle) {
        this.status = TaskStatus.COMPLETED;
        this.progress = 100;
        this.resultNovelId = novelId;
        this.resultNovelTitle = novelTitle;
        this.endTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 标记为失败
     */
    public void markAsFailed(String errorMessage) {
        this.status = TaskStatus.FAILED;
        this.errorMessage = errorMessage;
        this.endTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 获取执行耗时（秒）
     */
    public Long getDurationSeconds() {
        if (startTime == null) return null;
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).getSeconds();
    }

    /**
     * 格式化时间
     */
    public String getFormattedCreateTime() {
        return createTime != null ? createTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "";
    }

    // ========== Getters and Setters ==========

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
        this.updateTime = LocalDateTime.now();
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
        this.updateTime = LocalDateTime.now();
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
        this.updateTime = LocalDateTime.now();
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

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }

    public String getGeneratedTools() {
        return generatedTools;
    }

    public void setGeneratedTools(String generatedTools) {
        this.generatedTools = generatedTools;
        this.updateTime = LocalDateTime.now();
    }

    public String getGeneratedGameplay() {
        return generatedGameplay;
    }

    public void setGeneratedGameplay(String generatedGameplay) {
        this.generatedGameplay = generatedGameplay;
        this.updateTime = LocalDateTime.now();
    }

    public String getGeneratedWorldview() {
        return generatedWorldview;
    }

    public void setGeneratedWorldview(String generatedWorldview) {
        this.generatedWorldview = generatedWorldview;
        this.updateTime = LocalDateTime.now();
    }

    public String getGeneratedCharacters() {
        return generatedCharacters;
    }

    public void setGeneratedCharacters(String generatedCharacters) {
        this.generatedCharacters = generatedCharacters;
        this.updateTime = LocalDateTime.now();
    }

    public String getGeneratedOutline() {
        return generatedOutline;
    }

    public void setGeneratedOutline(String generatedOutline) {
        this.generatedOutline = generatedOutline;
        this.updateTime = LocalDateTime.now();
    }

    public String getGeneratedChapters() {
        return generatedChapters;
    }

    public void setGeneratedChapters(String generatedChapters) {
        this.generatedChapters = generatedChapters;
        this.updateTime = LocalDateTime.now();
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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        return "NovelGeneratorTask{" +
                "id=" + id +
                ", taskId='" + taskId + '\'' +
                ", status=" + status +
                ", currentStep='" + currentStep + '\'' +
                ", progress=" + progress +
                ", chapterCount=" + chapterCount +
                ", resultNovelId=" + resultNovelId +
                '}';
    }
}

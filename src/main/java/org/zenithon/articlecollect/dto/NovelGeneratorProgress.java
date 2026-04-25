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

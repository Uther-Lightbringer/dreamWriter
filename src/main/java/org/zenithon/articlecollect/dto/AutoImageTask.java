package org.zenithon.articlecollect.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 自动配图任务状态DTO
 */
public class AutoImageTask {

    private String taskId;
    private Long chapterId;
    private TaskStatus status;
    private int totalCount;
    private int completedCount;
    private List<ImageGenerationItem> items;
    private long startTime;
    private long endTime;
    private String errorMessage;

    public enum TaskStatus {
        PENDING,      // 等待中
        PROCESSING,   // 处理中
        COMPLETED,    // 已完成
        FAILED        // 失败
    }

    public static class ImageGenerationItem {
        private int index;
        private String position;
        private String description;
        private String prompt;
        private String imageUrl;
        private ItemStatus status;
        private String errorMessage;
        private int progress; // 0-100

        public enum ItemStatus {
            PENDING,      // 等待中
            GENERATING,   // 生成中
            COMPLETED,    // 已完成
            FAILED        // 失败
        }

        public ImageGenerationItem() {
            this.status = ItemStatus.PENDING;
            this.progress = 0;
        }

        public ImageGenerationItem(int index, String position, String description) {
            this.index = index;
            this.position = position;
            this.description = description;
            this.status = ItemStatus.PENDING;
            this.progress = 0;
        }

        // Getters and Setters
        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }

        public String getPosition() { return position; }
        public void setPosition(String position) { this.position = position; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        public ItemStatus getStatus() { return status; }
        public void setStatus(ItemStatus status) { this.status = status; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }
    }

    public AutoImageTask() {
        this.status = TaskStatus.PENDING;
        this.items = new ArrayList<>();
        this.startTime = System.currentTimeMillis();
    }

    public AutoImageTask(String taskId, Long chapterId) {
        this.taskId = taskId;
        this.chapterId = chapterId;
        this.status = TaskStatus.PENDING;
        this.items = new ArrayList<>();
        this.startTime = System.currentTimeMillis();
    }

    public void incrementCompleted() {
        this.completedCount++;
        if (this.completedCount >= this.totalCount) {
            this.status = TaskStatus.COMPLETED;
            this.endTime = System.currentTimeMillis();
        }
    }

    // Getters and Setters
    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public Long getChapterId() { return chapterId; }
    public void setChapterId(Long chapterId) { this.chapterId = chapterId; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

    public int getCompletedCount() { return completedCount; }
    public void setCompletedCount(int completedCount) { this.completedCount = completedCount; }

    public List<ImageGenerationItem> getItems() { return items; }
    public void setItems(List<ImageGenerationItem> items) { this.items = items; }
    public void addItem(ImageGenerationItem item) { this.items.add(item); }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}

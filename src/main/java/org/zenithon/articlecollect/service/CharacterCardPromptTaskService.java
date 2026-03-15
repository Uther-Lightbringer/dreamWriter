package org.zenithon.articlecollect.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 角色卡 AI 绘画提示词生成任务管理器
 * 使用长轮询方式通知前端任务完成状态
 */
@Service
public class CharacterCardPromptTaskService {
    
    private static final Logger logger = LoggerFactory.getLogger(CharacterCardPromptTaskService.class);
    
    /**
     * 存储进行中的任务，key: taskId, value: 任务状态
     */
    private final Map<String, TaskStatus> taskStatusMap = new ConcurrentHashMap<>();
    
    /**
     * 任务超时时间（毫秒）- 5 分钟
     */
    private static final long TASK_TIMEOUT_MS = 5 * 60 * 1000;
    
    /**
     * 清理过期任务的间隔时间（毫秒）- 1 分钟
     */
    private static final long CLEANUP_INTERVAL_MS = 60 * 1000;
    
    /**
     * 上一次清理时间
     */
    private long lastCleanupTime = System.currentTimeMillis();
    
    /**
     * 任务状态枚举
     */
    public enum TaskState {
        PENDING,      // 等待中
        PROCESSING,   // 处理中
        COMPLETED,    // 已完成
        FAILED        // 失败
    }
    
    /**
     * 任务状态类
     */
    public static class TaskStatus {
        private final String taskId;
        private final Long novelId;
        private final Long characterId;
        private final String characterName;
        private TaskState state;
        private String message;
        private Object data;
        private long createTime;
        private long updateTime;
        
        public TaskStatus(String taskId, Long novelId, Long characterId, String characterName) {
            this.taskId = taskId;
            this.novelId = novelId;
            this.characterId = characterId;
            this.characterName = characterName;
            this.state = TaskState.PENDING;
            this.createTime = System.currentTimeMillis();
            this.updateTime = System.currentTimeMillis();
        }
        
        // Getters and Setters
        public String getTaskId() { return taskId; }
        public Long getNovelId() { return novelId; }
        public Long getCharacterId() { return characterId; }
        public String getCharacterName() { return characterName; }
        public TaskState getState() { return state; }
        public String getMessage() { return message; }
        public Object getData() { return data; }
        
        public void setState(TaskState state) {
            this.state = state;
            this.updateTime = System.currentTimeMillis();
        }
        
        public void setMessage(String message) {
            this.message = message;
            this.updateTime = System.currentTimeMillis();
        }
        
        public void setData(Object data) {
            this.data = data;
            this.updateTime = System.currentTimeMillis();
        }
        
        public boolean isTimeout() {
            return System.currentTimeMillis() - createTime > TASK_TIMEOUT_MS;
        }
    }
    
    /**
     * 创建新任务
     */
    public TaskStatus createTask(Long novelId, Long characterId, String characterName) {
        cleanupExpiredTasks();
        
        String taskId = generateTaskId(novelId, characterId);
        TaskStatus status = new TaskStatus(taskId, novelId, characterId, characterName);
        taskStatusMap.put(taskId, status);
        
        logger.info("创建提示词生成任务：taskId={}, novelId={}, characterId={}, characterName={}", 
            taskId, novelId, characterId, characterName);
        
        return status;
    }
    
    /**
     * 更新任务状态为处理中
     */
    public void updateTaskProcessing(String taskId) {
        TaskStatus status = taskStatusMap.get(taskId);
        if (status != null) {
            status.setState(TaskState.PROCESSING);
            status.setMessage("正在生成 AI 绘画提示词...");
            logger.info("任务处理中：taskId={}", taskId);
        }
    }
    
    /**
     * 更新任务状态为完成
     */
    public void completeTask(String taskId, Object data) {
        TaskStatus status = taskStatusMap.get(taskId);
        if (status != null) {
            status.setState(TaskState.COMPLETED);
            status.setMessage("提示词生成成功");
            status.setData(data);
            logger.info("任务完成：taskId={}", taskId);
        }
    }
    
    /**
     * 更新任务状态为失败
     */
    public void failTask(String taskId, String errorMessage) {
        TaskStatus status = taskStatusMap.get(taskId);
        if (status != null) {
            status.setState(TaskState.FAILED);
            status.setMessage(errorMessage != null ? errorMessage : "提示词生成失败");
            logger.error("任务失败：taskId={}, error={}", taskId, errorMessage);
        }
    }
    
    /**
     * 获取任务状态（用于长轮询）
     * @param taskId 任务 ID
     * @param timeout 等待时间（秒）
     * @return 任务状态
     */
    public TaskStatus waitForTaskCompletion(String taskId, int timeout) {
        long startTime = System.currentTimeMillis();
        long timeoutMs = timeout * 1000L;
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            TaskStatus status = taskStatusMap.get(taskId);
            
            if (status == null) {
                return null;
            }
            
            // 如果任务已完成、失败或超时，立即返回
            if (status.getState() == TaskState.COMPLETED || 
                status.getState() == TaskState.FAILED ||
                status.isTimeout()) {
                return status;
            }
            
            // 等待一段时间后再次检查
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return status;
            }
        }
        
        // 超时后返回当前状态（可能还是 PENDING 或 PROCESSING）
        return taskStatusMap.get(taskId);
    }
    
    /**
     * 获取任务状态（不等待）
     */
    public TaskStatus getTaskStatus(String taskId) {
        return taskStatusMap.get(taskId);
    }
    
    /**
     * 删除任务（完成后清理）
     */
    public void removeTask(String taskId) {
        taskStatusMap.remove(taskId);
        logger.debug("删除任务：taskId={}", taskId);
    }
    
    /**
     * 生成任务 ID
     */
    private String generateTaskId(Long novelId, Long characterId) {
        return "prompt-task-" + novelId + "-" + characterId + "-" + System.currentTimeMillis();
    }
    
    /**
     * 清理过期任务
     */
    private void cleanupExpiredTasks() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime < CLEANUP_INTERVAL_MS) {
            return;
        }
        
        taskStatusMap.entrySet().removeIf(entry -> entry.getValue().isTimeout());
        lastCleanupTime = now;
        
        logger.debug("清理过期任务，当前任务数：{}", taskStatusMap.size());
    }
}

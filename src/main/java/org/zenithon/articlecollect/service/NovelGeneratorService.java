package org.zenithon.articlecollect.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.zenithon.articlecollect.dto.NovelGeneratorProgress;
import org.zenithon.articlecollect.dto.NovelGeneratorRequest;
import org.zenithon.articlecollect.entity.NovelGeneratorTask;
import org.zenithon.articlecollect.repository.NovelGeneratorTaskRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 小说生成器服务
 * 对外提供的主要接口
 */
@Service
public class NovelGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(NovelGeneratorService.class);

    private final NovelGeneratorTaskRepository taskRepository;
    private final NovelGeneratorAsyncService asyncService;

    public NovelGeneratorService(
            NovelGeneratorTaskRepository taskRepository,
            NovelGeneratorAsyncService asyncService) {
        this.taskRepository = taskRepository;
        this.asyncService = asyncService;
    }

    /**
     * 启动生成任务
     * @param request 生成请求参数
     * @return 任务ID
     */
    public String startGeneration(NovelGeneratorRequest request) {
        // 验证请求参数
        if (!request.isValid()) {
            throw new IllegalArgumentException("请求参数无效");
        }

        // 生成任务ID
        String taskId = generateTaskId();

        // 创建任务记录
        NovelGeneratorTask task = new NovelGeneratorTask(taskId);
        task.setChapterCount(request.getChapterCount());
        task.setKeyword(request.getKeyword());
        task.setGenre(request.getGenre());
        task.setRequires(request.getRequires());
        task.setNewWorld(request.getNewWorld());
        task.setRoleCount(request.getRoleCount());
        task.setGameplay(request.getGameplay());
        task.setTools(request.getTools());
        task.setPointOfView(request.getPointOfView());
        task.setProtagonist(request.getProtagonist());
        task.setLanguageStyle(request.getLanguageStyle());
        task.setWordsPerChapter(request.getWordsPerChapter());

        task = taskRepository.save(task);

        logger.info("创建生成任务: taskId={}, chapterCount={}", taskId, request.getChapterCount());

        // 异步执行生成任务
        asyncService.executeGenerationTask(taskId, request);

        return taskId;
    }

    /**
     * 生成任务ID
     */
    private String generateTaskId() {
        return "task_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 查询任务进度
     * @param taskId 任务ID
     * @return 进度信息
     */
    public NovelGeneratorProgress getProgress(String taskId) {
        Optional<NovelGeneratorTask> taskOpt = taskRepository.findByTaskId(taskId);
        if (taskOpt.isEmpty()) {
            throw new RuntimeException("任务不存在: " + taskId);
        }
        return new NovelGeneratorProgress(taskOpt.get());
    }

    /**
     * 获取所有任务列表
     * @return 任务列表
     */
    public List<NovelGeneratorProgress> getAllTasks() {
        return taskRepository.findAllByOrderByCreateTimeDesc()
                .stream()
                .map(NovelGeneratorProgress::new)
                .collect(Collectors.toList());
    }

    /**
     * 获取最近的任务
     * @param limit 数量限制
     * @return 任务列表
     */
    public List<NovelGeneratorProgress> getRecentTasks(int limit) {
        List<NovelGeneratorTask> tasks = taskRepository.findAllByOrderByCreateTimeDesc();
        return tasks.stream()
                .limit(limit)
                .map(NovelGeneratorProgress::new)
                .collect(Collectors.toList());
    }

    /**
     * 删除任务记录
     * @param taskId 任务ID
     */
    public void deleteTask(String taskId) {
        Optional<NovelGeneratorTask> taskOpt = taskRepository.findByTaskId(taskId);
        if (taskOpt.isPresent()) {
            NovelGeneratorTask task = taskOpt.get();
            // 只能删除已完成或失败的任务
            if (task.getStatus() == NovelGeneratorTask.TaskStatus.RUNNING) {
                throw new RuntimeException("无法删除正在运行的任务");
            }
            taskRepository.delete(task);
            logger.info("删除任务记录: taskId={}", taskId);
        }
    }

    /**
     * 检查任务是否存在
     * @param taskId 任务ID
     * @return 是否存在
     */
    public boolean taskExists(String taskId) {
        return taskRepository.existsByTaskId(taskId);
    }
}

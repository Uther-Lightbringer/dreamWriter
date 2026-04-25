package org.zenithon.articlecollect.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.zenithon.articlecollect.dto.NovelGeneratorProgress;
import org.zenithon.articlecollect.dto.NovelGeneratorRequest;
import org.zenithon.articlecollect.entity.NovelGeneratorTask;
import org.zenithon.articlecollect.repository.NovelGeneratorTaskRepository;
import org.zenithon.articlecollect.service.NovelGeneratorService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 小说生成器控制器
 * 提供REST API和页面入口
 */
@Controller
public class NovelGeneratorController {

    private static final Logger logger = LoggerFactory.getLogger(NovelGeneratorController.class);

    private final NovelGeneratorService generatorService;
    private final NovelGeneratorTaskRepository taskRepository;
    private final Executor sseExecutor = Executors.newCachedThreadPool();

    @Autowired
    public NovelGeneratorController(
            NovelGeneratorService generatorService,
            NovelGeneratorTaskRepository taskRepository) {
        this.generatorService = generatorService;
        this.taskRepository = taskRepository;
    }

    // ==================== 页面入口 ====================

    /**
     * 小说生成器页面
     */
    @GetMapping("/novel-generator")
    public String novelGeneratorPage() {
        return "novel-generator";
    }

    /**
     * 小说生成器历史页面
     */
    @GetMapping("/novel-generator-history")
    public String novelGeneratorHistoryPage() {
        return "novel-generator-history";
    }

    // ==================== REST API ====================

    /**
     * 启动生成任务
     */
    @PostMapping("/api/novel-generator/start")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> startGeneration(@RequestBody NovelGeneratorRequest request) {
        try {
            logger.info("收到生成请求: chapterCount={}, genre={}", request.getChapterCount(), request.getGenre());

            // 验证请求
            if (!request.isValid()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "请求参数无效");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            // 启动任务
            String taskId = generatorService.startGeneration(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", taskId);
            response.put("status", "PENDING");
            response.put("message", "任务已创建，正在后台执行");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("启动生成任务失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 查询任务进度
     */
    @GetMapping("/api/novel-generator/progress/{taskId}")
    @ResponseBody
    public ResponseEntity<NovelGeneratorProgress> getProgress(@PathVariable String taskId) {
        try {
            NovelGeneratorProgress progress = generatorService.getProgress(taskId);
            return ResponseEntity.ok(progress);
        } catch (Exception e) {
            logger.error("查询进度失败: taskId={}", taskId, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * SSE实时进度流
     */
    @GetMapping(value = "/api/novel-generator/stream/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter streamProgress(@PathVariable String taskId) {
        logger.info("建立SSE连接: taskId={}", taskId);

        // 创建SSE发射器，超时时间30分钟
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        sseExecutor.execute(() -> {
            try {
                int lastProgress = -1;
                String lastStep = "";

                while (true) {
                    NovelGeneratorTask task = taskRepository.findByTaskId(taskId).orElse(null);

                    if (task == null) {
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data("{\"error\": \"任务不存在\"}"));
                        emitter.complete();
                        break;
                    }

                    // 只有进度或步骤变化时才发送
                    if (task.getProgress() != lastProgress || !task.getCurrentStep().equals(lastStep)) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("step", task.getCurrentStep());
                        data.put("progress", task.getProgress());
                        data.put("status", task.getStatus().name());

                        if (task.getResultNovelId() != null) {
                            data.put("novelId", task.getResultNovelId());
                            data.put("novelTitle", task.getResultNovelTitle());
                        }

                        if (task.getErrorMessage() != null) {
                            data.put("error", task.getErrorMessage());
                        }

                        emitter.send(SseEmitter.event()
                                .name("progress")
                                .data(data));

                        lastProgress = task.getProgress();
                        lastStep = task.getCurrentStep();
                    }

                    // 如果任务完成或失败，结束SSE
                    if (task.getStatus() == NovelGeneratorTask.TaskStatus.COMPLETED) {
                        Map<String, Object> completeData = new HashMap<>();
                        completeData.put("step", "完成");
                        completeData.put("progress", 100);
                        completeData.put("status", "COMPLETED");
                        completeData.put("novelId", task.getResultNovelId());
                        completeData.put("novelTitle", task.getResultNovelTitle());

                        emitter.send(SseEmitter.event()
                                .name("complete")
                                .data(completeData));
                        emitter.complete();
                        break;
                    }

                    if (task.getStatus() == NovelGeneratorTask.TaskStatus.FAILED) {
                        Map<String, Object> errorData = new HashMap<>();
                        errorData.put("step", "失败");
                        errorData.put("progress", task.getProgress());
                        errorData.put("status", "FAILED");
                        errorData.put("error", task.getErrorMessage());

                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data(errorData));
                        emitter.complete();
                        break;
                    }

                    // 等待2秒后再次查询
                    Thread.sleep(2000);
                }
            } catch (IOException e) {
                logger.warn("SSE连接断开: taskId={}", taskId);
            } catch (Exception e) {
                logger.error("SSE流异常: taskId={}", taskId, e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("{\"error\": \"" + e.getMessage() + "\"}"));
                } catch (IOException ignored) {
                }
                emitter.completeWithError(e);
            }
        });

        // 设置超时和完成回调
        emitter.onTimeout(() -> {
            logger.warn("SSE连接超时: taskId={}", taskId);
        });

        emitter.onCompletion(() -> {
            logger.info("SSE连接关闭: taskId={}", taskId);
        });

        return emitter;
    }

    /**
     * 获取所有任务列表
     */
    @GetMapping("/api/novel-generator/tasks")
    @ResponseBody
    public ResponseEntity<List<NovelGeneratorProgress>> getAllTasks() {
        List<NovelGeneratorProgress> tasks = generatorService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }

    /**
     * 获取最近任务
     */
    @GetMapping("/api/novel-generator/tasks/recent")
    @ResponseBody
    public ResponseEntity<List<NovelGeneratorProgress>> getRecentTasks(
            @RequestParam(defaultValue = "10") int limit) {
        List<NovelGeneratorProgress> tasks = generatorService.getRecentTasks(limit);
        return ResponseEntity.ok(tasks);
    }

    /**
     * 删除任务记录
     */
    @DeleteMapping("/api/novel-generator/tasks/{taskId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteTask(@PathVariable String taskId) {
        try {
            generatorService.deleteTask(taskId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "任务已删除");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("删除任务失败: taskId={}", taskId, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 检查任务是否存在
     */
    @GetMapping("/api/novel-generator/tasks/{taskId}/exists")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> checkTaskExists(@PathVariable String taskId) {
        boolean exists = generatorService.taskExists(taskId);

        Map<String, Object> response = new HashMap<>();
        response.put("exists", exists);

        return ResponseEntity.ok(response);
    }
}

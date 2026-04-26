package org.zenithon.articlecollect.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.zenithon.articlecollect.config.EvoLinkConfig;
import org.zenithon.articlecollect.dto.AutoImageTask;
import org.zenithon.articlecollect.dto.AutoImageTask.ImageGenerationItem;
import org.zenithon.articlecollect.entity.Chapter;
import org.zenithon.articlecollect.entity.CharacterCardEntity;
import org.zenithon.articlecollect.repository.ChapterRepository;
import org.zenithon.articlecollect.repository.CharacterCardRepository;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 自动配图任务管理服务
 */
@Service
public class AutoImageTaskService {

    private static final Logger logger = LoggerFactory.getLogger(AutoImageTaskService.class);

    // 存储所有任务（内存存储，重启后丢失）
    private final Map<String, AutoImageTask> taskMap = new ConcurrentHashMap<>();

    @Autowired
    private ChapterImageService chapterImageService;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private CharacterCardRepository characterCardRepository;

    @Autowired
    private EvoLinkConfig evoLinkConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SystemConfigService systemConfigService;

    // 创建固定大小的线程池用于并发生成图片
    private final ExecutorService imageGenerationExecutor = Executors.newFixedThreadPool(4);

    /**
     * 创建新的自动配图任务
     */
    public String createTask(Long chapterId, List<Map<String, Object>> positions) {
        return createTask(chapterId, positions, null);
    }

    /**
     * 创建新的自动配图任务（带画风参数）
     */
    public String createTask(Long chapterId, List<Map<String, Object>> positions, String style) {
        String taskId = UUID.randomUUID().toString();
        AutoImageTask task = new AutoImageTask(taskId, chapterId);
        task.setStyle(style != null ? style : AIPromptService.DEFAULT_STYLE);
        task.setTotalCount(positions.size());

        // 初始化任务项
        for (int i = 0; i < positions.size(); i++) {
            Map<String, Object> pos = positions.get(i);
            ImageGenerationItem item = new ImageGenerationItem(
                i,
                (String) pos.get("position"),
                (String) pos.get("description")
            );
            task.addItem(item);
        }

        taskMap.put(taskId, task);
        logger.info("创建自动配图任务: {}, 章节ID: {}, 图片数量: {}", taskId, chapterId, positions.size());
        return taskId;
    }

    /**
     * 获取任务状态
     */
    public AutoImageTask getTask(String taskId) {
        return taskMap.get(taskId);
    }

    /**
     * 异步执行图片生成任务
     */
    @Async("characterCardTaskExecutor")
    public void executeTaskAsync(String taskId) {
        AutoImageTask task = taskMap.get(taskId);
        if (task == null) {
            logger.error("任务不存在: {}", taskId);
            return;
        }

        try {
            logger.info("开始执行自动配图任务: {}", taskId);
            task.setStatus(AutoImageTask.TaskStatus.PROCESSING);

            // 获取角色卡信息
            List<CharacterCardEntity> characterCards = getCharacterCards(task.getChapterId());

            // 使用CountDownLatch等待所有任务完成
            CountDownLatch latch = new CountDownLatch(task.getItems().size());

            // 并发生成所有图片
            for (ImageGenerationItem item : task.getItems()) {
                imageGenerationExecutor.submit(() -> {
                    try {
                        generateImageForItem(task, item, characterCards);
                    } catch (Exception e) {
                        logger.error("生成图片失败: {}", e.getMessage(), e);
                        item.setStatus(ImageGenerationItem.ItemStatus.FAILED);
                        item.setErrorMessage(e.getMessage());
                    } finally {
                        latch.countDown();
                        task.incrementCompleted();
                    }
                });
            }

            // 等待所有任务完成
            latch.await();
            task.setStatus(AutoImageTask.TaskStatus.COMPLETED);
            task.setEndTime(System.currentTimeMillis());
            logger.info("自动配图任务完成: {}, 耗时: {}ms", taskId,
                task.getEndTime() - task.getStartTime());

        } catch (Exception e) {
            logger.error("自动配图任务失败: {}", taskId, e);
            task.setStatus(AutoImageTask.TaskStatus.FAILED);
            task.setErrorMessage(e.getMessage());
        }
    }

    /**
     * 为单个项目生成图片
     */
    private void generateImageForItem(AutoImageTask task, ImageGenerationItem item,
                                       List<CharacterCardEntity> characterCards) {
        logger.info("开始生成图片 {}/{}: {}", item.getIndex() + 1, task.getTotalCount(),
            item.getPosition().substring(0, Math.min(50, item.getPosition().length())));

        item.setStatus(ImageGenerationItem.ItemStatus.GENERATING);
        item.setProgress(10);

        try {
            // 1. 生成AI绘画提示词（包含角色卡信息和画风）
            String prompt = chapterImageService.generateImagePromptWithCharacters(
                item.getDescription(), characterCards, task.getStyle());
            if (prompt == null || prompt.trim().isEmpty()) {
                throw new RuntimeException("提示词生成失败");
            }
            item.setPrompt(prompt);
            item.setProgress(30);
            logger.info("提示词生成完成: {}", prompt.substring(0, Math.min(100, prompt.length())));

            // 2. 使用EvoLink生成图片
            EvoLinkImageService imageService = new EvoLinkImageService(
                evoLinkConfig, restTemplate, objectMapper, systemConfigService);
            String evoTaskId = imageService.generateImage(prompt, "16:9");
            item.setProgress(50);

            // 3. 等待图片生成完成并更新进度
            String imageUrl = waitForImageWithProgress(imageService, evoTaskId, item);
            if (imageUrl == null) {
                throw new RuntimeException("图片生成失败");
            }
            item.setProgress(80);

            // 4. 下载并保存图片到本地
            String localPath = downloadAndSaveImage(task.getChapterId(), imageUrl, item.getIndex());
            item.setImageUrl(localPath);
            item.setStatus(ImageGenerationItem.ItemStatus.COMPLETED);
            item.setProgress(100);

            logger.info("图片生成完成: {}", localPath);

        } catch (Exception e) {
            logger.error("生成图片失败: {}", e.getMessage(), e);
            item.setStatus(ImageGenerationItem.ItemStatus.FAILED);
            item.setErrorMessage(e.getMessage());
        }
    }

    /**
     * 等待图片生成并更新进度
     */
    private String waitForImageWithProgress(EvoLinkImageService imageService,
                                             String evoTaskId, ImageGenerationItem item) {
        long timeout = 5 * 60 * 1000; // 5分钟超时
        long startTime = System.currentTimeMillis();
        int baseProgress = 50;

        while (System.currentTimeMillis() - startTime < timeout) {
            try {
                EvoLinkImageService.TaskStatus status = imageService.getTaskStatus(evoTaskId);

                if (status.isCompleted()) {
                    item.setProgress(80);
                    return status.getImageUrl();
                }

                if (status.isFailed()) {
                    logger.error("EvoLink图片生成失败: {}", status.getError());
                    return null;
                }

                // 更新进度（从50%到80%）
                int progress = baseProgress + (int)(status.getProgress() * 0.3);
                item.setProgress(Math.min(progress, 79));

                // 等待2秒后再次检查
                Thread.sleep(2000);

            } catch (Exception e) {
                logger.error("检查任务状态失败: {}", e.getMessage(), e);
                return null;
            }
        }

        logger.error("等待图片生成超时");
        return null;
    }

    /**
     * 下载并保存图片
     */
    private String downloadAndSaveImage(Long chapterId, String imageUrl, int index) {
        try {
            // 下载图片
            org.springframework.http.ResponseEntity<byte[]> response =
                restTemplate.getForEntity(imageUrl, byte[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // 获取章节信息
                Chapter chapter = chapterRepository.findById(chapterId)
                    .orElseThrow(() -> new RuntimeException("章节不存在"));

                // 保存图片
                return org.zenithon.articlecollect.util.FileUploadUtil.saveAutoGeneratedImage(
                    response.getBody(),
                    chapterId,
                    chapter.getTitle() + "-" + index
                );
            }

            return null;

        } catch (Exception e) {
            logger.error("下载并保存图片失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取角色卡列表
     */
    private List<CharacterCardEntity> getCharacterCards(Long chapterId) {
        try {
            Chapter chapter = chapterRepository.findById(chapterId).orElse(null);
            if (chapter == null) {
                return new ArrayList<>();
            }

            List<CharacterCardEntity> cards = characterCardRepository
                .findByNovelIdOrderBySortOrderAsc(chapter.getNovelId());
            return cards != null ? cards : new ArrayList<>();

        } catch (Exception e) {
            logger.error("获取角色卡失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 清理已完成的任务（保留1小时）
     */
    public void cleanupOldTasks() {
        long oneHourAgo = System.currentTimeMillis() - 3600 * 1000;
        taskMap.entrySet().removeIf(entry -> {
            AutoImageTask task = entry.getValue();
            return (task.getStatus() == AutoImageTask.TaskStatus.COMPLETED ||
                    task.getStatus() == AutoImageTask.TaskStatus.FAILED) &&
                    task.getEndTime() > 0 && task.getEndTime() < oneHourAgo;
        });
    }
}

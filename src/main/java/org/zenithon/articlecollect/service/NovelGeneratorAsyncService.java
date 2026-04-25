package org.zenithon.articlecollect.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zenithon.articlecollect.dto.CharacterCard;
import org.zenithon.articlecollect.dto.GeneratedOutline;
import org.zenithon.articlecollect.dto.NovelGeneratorRequest;
import org.zenithon.articlecollect.entity.Chapter;
import org.zenithon.articlecollect.entity.Novel;
import org.zenithon.articlecollect.entity.NovelGeneratorTask;
import org.zenithon.articlecollect.repository.ChapterRepository;
import org.zenithon.articlecollect.repository.NovelGeneratorTaskRepository;
import org.zenithon.articlecollect.repository.NovelRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 小说生成器异步执行服务
 * 负责在后台执行完整的生成流程
 */
@Service
public class NovelGeneratorAsyncService {

    private static final Logger logger = LoggerFactory.getLogger(NovelGeneratorAsyncService.class);

    private final NovelGeneratorTaskRepository taskRepository;
    private final NovelGeneratorStepService stepService;
    private final NovelRepository novelRepository;
    private final ChapterRepository chapterRepository;
    private final CharacterCardService characterCardService;
    private final ObjectMapper objectMapper;
    private final Executor novelGeneratorTaskExecutor;

    @Autowired
    public NovelGeneratorAsyncService(
            NovelGeneratorTaskRepository taskRepository,
            NovelGeneratorStepService stepService,
            NovelRepository novelRepository,
            ChapterRepository chapterRepository,
            CharacterCardService characterCardService,
            ObjectMapper objectMapper,
            Executor novelGeneratorTaskExecutor) {
        this.taskRepository = taskRepository;
        this.stepService = stepService;
        this.novelRepository = novelRepository;
        this.chapterRepository = chapterRepository;
        this.characterCardService = characterCardService;
        this.objectMapper = objectMapper;
        this.novelGeneratorTaskExecutor = novelGeneratorTaskExecutor;
    }

    /**
     * 异步执行生成任务
     */
    @Async("novelGeneratorTaskExecutor")
    @Transactional
    public void executeGenerationTask(String taskId, NovelGeneratorRequest request) {
        NovelGeneratorTask task = null;
        try {
            task = taskRepository.findByTaskId(taskId)
                    .orElseThrow(() -> new RuntimeException("任务不存在: " + taskId));

            // 标记为运行中
            task.markAsRunning();
            taskRepository.save(task);

            // ========== Step 1: 工具生成 (5%) ==========
            updateProgress(task, "正在生成工具列表...", 5);
            String tools = stepService.generateTools(request.getTools());
            task.setGeneratedTools(tools);
            taskRepository.save(task);

            // ========== Step 2: 玩法生成 (10%) ==========
            updateProgress(task, "正在生成玩法列表...", 10);
            String gameplay = stepService.generateGameplay(request.getGameplay());
            task.setGeneratedGameplay(gameplay);
            taskRepository.save(task);

            // ========== Step 3: 世界观生成 (15%) ==========
            String worldview;
            if (Boolean.TRUE.equals(request.getNewWorld())) {
                updateProgress(task, "正在生成世界观...", 15);
                worldview = stepService.generateWorldview(request.getKeyword());
            } else {
                updateProgress(task, "使用默认世界观", 15);
                worldview = "现代社会";
            }
            task.setGeneratedWorldview(worldview);
            taskRepository.save(task);

            // ========== Step 4: 角色卡生成 (20%) ==========
            updateProgress(task, "正在生成角色卡...", 20);
            List<CharacterCard> characterCards = stepService.generateCharacterCards(
                    request.getKeyword(),
                    worldview,
                    request.getRoleCount(),
                    tools,
                    gameplay,
                    request.getProtagonist()
            );
            String charactersJson = objectMapper.writeValueAsString(characterCards);
            task.setGeneratedCharacters(charactersJson);
            taskRepository.save(task);

            // ========== Step 5: 大纲和章节生成 (25%) ==========
            updateProgress(task, "正在生成大纲和章节列表...", 25);
            GeneratedOutline outline = stepService.generateOutline(
                    request.getKeyword(),
                    worldview,
                    charactersJson,
                    request.getChapterCount(),
                    request.getGenre(),
                    request.getRequires(),
                    tools,
                    gameplay,
                    request.getPointOfView(),
                    request.getProtagonist()
            );
            task.setGeneratedOutline(outline.getOutline());
            taskRepository.save(task);

            // ========== Step 6: 创建小说记录 (30%) ==========
            updateProgress(task, "正在创建小说...", 28);
            String novelTitle = stepService.generateNovelTitle(outline.getOutline(), request.getPointOfView());

            updateProgress(task, "正在保存小说数据...", 30);
            Novel novel = new Novel(novelTitle);
            novel.setWorldView(worldview);
            novel.setDescription(request.getKeyword());
            novel = novelRepository.save(novel);

            task.setResultNovelId(novel.getId());
            task.setResultNovelTitle(novelTitle);
            taskRepository.save(task);

            // 保存角色卡
            updateProgress(task, "正在保存角色卡...", 32);
            saveCharacterCards(novel.getId(), characterCards);

            // ========== Step 7: 章节迭代扩写 (35% - 90%) ==========
            List<GeneratedOutline.ChapterInfo> chapters = outline.getChapters();
            if (chapters == null || chapters.isEmpty()) {
                throw new RuntimeException("章节列表为空");
            }

            int totalChapters = chapters.size();
            int baseProgress = 35;
            int progressRange = 55; // 35% to 90%

            // 并行处理章节生成
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            List<Chapter> generatedChapters = new ArrayList<>();
            int[] completedCount = {0};

            for (int i = 0; i < chapters.size(); i++) {
                final int index = i;
                final GeneratedOutline.ChapterInfo chapterInfo = chapters.get(i);

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        String chapterContent = generateChapterContent(
                                chapterInfo, outline.getOutline(),
                                charactersJson, worldview, tools, gameplay,
                                request.getGenre(), request.getRequires(), request.getPointOfView()
                        );

                        Chapter chapter = new Chapter();
                        chapter.setNovelId(novel.getId());
                        chapter.setTitle(chapterInfo.getSection());
                        chapter.setContent(chapterContent);
                        chapter.setChapterNumber(index + 1);
                        chapter.setStorySummary(chapterInfo.getDescription());

                        synchronized (generatedChapters) {
                            generatedChapters.add(chapter);
                            completedCount[0]++;

                            // 更新进度
                            int currentProgress = baseProgress + (int) ((completedCount[0] / (double) totalChapters) * progressRange);
                            updateProgress(task, "正在生成第 " + completedCount[0] + "/" + totalChapters + " 章: " + chapterInfo.getSection(), currentProgress);
                            taskRepository.save(task);
                        }
                    } catch (Exception e) {
                        logger.error("生成章节失败 [{}]: {}", chapterInfo.getSection(), e.getMessage(), e);
                        throw new RuntimeException("生成章节失败: " + chapterInfo.getSection(), e);
                    }
                }, novelGeneratorTaskExecutor);

                futures.add(future);
            }

            // 等待所有章节生成完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // 按章节号排序并保存
            generatedChapters.sort((a, b) -> a.getChapterNumber() - b.getChapterNumber());
            for (Chapter chapter : generatedChapters) {
                chapterRepository.save(chapter);
            }

            // ========== Step 8: 完成 (100%) ==========
            updateProgress(task, "生成完成！", 100);
            task.markAsCompleted(novel.getId(), novelTitle);
            taskRepository.save(task);

            logger.info("小说生成任务完成: taskId={}, novelId={}, title={}", taskId, novel.getId(), novelTitle);

        } catch (Exception e) {
            logger.error("小说生成任务失败: taskId={}, error={}", taskId, e.getMessage(), e);
            if (task != null) {
                task.markAsFailed(e.getMessage());
                taskRepository.save(task);
            }
        }
    }

    /**
     * 生成单个章节内容
     */
    private String generateChapterContent(
            GeneratedOutline.ChapterInfo chapterInfo,
            String outline,
            String charactersJson,
            String worldview,
            String tools,
            String gameplay,
            String genre,
            String requires,
            String pointOfView) throws Exception {

        // 提取相关上下文
        String relevantContext = stepService.extractRelevantContext(
                charactersJson, worldview, chapterInfo
        );

        // 扩写章节
        String expandedContent = stepService.expandChapter(
                chapterInfo, outline, relevantContext,
                tools, gameplay, genre, requires, pointOfView
        );

        // 去除AI味润色
        String polishedContent = stepService.polishContent(expandedContent);

        // 格式化（句号后换行）
        return stepService.formatChapterContent(polishedContent);
    }

    /**
     * 保存角色卡
     */
    private void saveCharacterCards(Long novelId, List<CharacterCard> characterCards) {
        for (int i = 0; i < characterCards.size(); i++) {
            CharacterCard card = characterCards.get(i);
            try {
                characterCardService.addCharacterCard(novelId, card, i);
            } catch (Exception e) {
                logger.error("保存角色卡失败: {}", card.getName(), e);
            }
        }
    }

    /**
     * 更新进度
     */
    private void updateProgress(NovelGeneratorTask task, String step, int progress) {
        task.updateProgress(step, progress);
        logger.info("任务进度更新: taskId={}, step={}, progress={}%", task.getTaskId(), step, progress);
    }
}

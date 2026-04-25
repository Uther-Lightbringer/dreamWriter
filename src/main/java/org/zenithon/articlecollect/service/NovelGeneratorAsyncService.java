package org.zenithon.articlecollect.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
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
    private final CharacterCardAsyncService characterCardAsyncService;
    private final ObjectMapper objectMapper;
    private final Executor novelGeneratorTaskExecutor;

    @Autowired
    public NovelGeneratorAsyncService(
            NovelGeneratorTaskRepository taskRepository,
            NovelGeneratorStepService stepService,
            NovelRepository novelRepository,
            ChapterRepository chapterRepository,
            CharacterCardService characterCardService,
            CharacterCardAsyncService characterCardAsyncService,
            ObjectMapper objectMapper,
            Executor novelGeneratorTaskExecutor) {
        this.taskRepository = taskRepository;
        this.stepService = stepService;
        this.novelRepository = novelRepository;
        this.chapterRepository = chapterRepository;
        this.characterCardService = characterCardService;
        this.characterCardAsyncService = characterCardAsyncService;
        this.objectMapper = objectMapper;
        this.novelGeneratorTaskExecutor = novelGeneratorTaskExecutor;
    }

    /**
     * 异步执行生成任务
     */
    @Async("novelGeneratorTaskExecutor")
    public void executeGenerationTask(String taskId, NovelGeneratorRequest request) {
        NovelGeneratorTask task = null;
        try {
            // 设置当前任务使用的 AI 模型
            String aiModel = request.getAiModel();
            if (aiModel != null && !aiModel.isEmpty()) {
                stepService.setCurrentModel(aiModel);
                logger.info("任务 {} 使用模型: {}", taskId, aiModel);
            }

            task = taskRepository.findByTaskId(taskId)
                    .orElseThrow(() -> new RuntimeException("任务不存在: " + taskId));

            // 标记为运行中
            task.markAsRunning();
            // 保存选择的模型到任务实体
            task.setAiModel(aiModel);
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
            // 用生成的大纲作为小说概述
            novel.setDescription(outline.getOutline());
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

            // 创建final副本供Lambda使用
            final Long novelId = novel.getId();
            final String taskIdValue = task.getTaskId();
            final String outlineText = outline.getOutline();
            final String finalCharactersJson = charactersJson;
            final String finalWorldview = worldview;
            final String finalTools = tools;
            final String finalGameplay = gameplay;
            final String genre = request.getGenre();
            final String requires = request.getRequires();
            final String pointOfView = request.getPointOfView();
            final String languageStyle = request.getLanguageStyle();
            final int wordsPerChapter = request.getWordsPerChapter() != null ? request.getWordsPerChapter() : 3000;

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
                                chapterInfo, outlineText,
                                finalCharactersJson, finalWorldview, finalTools, finalGameplay,
                                genre, requires, pointOfView, languageStyle, wordsPerChapter
                        );

                        Chapter chapter = new Chapter();
                        chapter.setNovelId(novelId);
                        chapter.setTitle(chapterInfo.getSection());
                        chapter.setContent(chapterContent);
                        chapter.setChapterNumber(index + 1);
                        chapter.setStorySummary(chapterInfo.getDescription());

                        synchronized (generatedChapters) {
                            generatedChapters.add(chapter);
                            completedCount[0]++;

                            // 更新进度
                            int currentProgress = baseProgress + (int) ((completedCount[0] / (double) totalChapters) * progressRange);
                            // 重新查询task以避免并发问题
                            NovelGeneratorTask currentTask = taskRepository.findByTaskId(taskIdValue).orElse(null);
                            if (currentTask != null) {
                                updateProgress(currentTask, "正在生成第 " + completedCount[0] + "/" + totalChapters + " 章: " + chapterInfo.getSection(), currentProgress);
                                taskRepository.save(currentTask);
                            }
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
        } finally {
            // 清除当前任务的模型设置
            stepService.clearCurrentModel();
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
            String pointOfView,
            String languageStyle,
            int wordsPerChapter) throws Exception {

        // 提取相关上下文
        String relevantContext = stepService.extractRelevantContext(
                charactersJson, worldview, chapterInfo
        );

        // 扩写章节
        String expandedContent = stepService.expandChapter(
                chapterInfo, outline, relevantContext,
                tools, gameplay, genre, requires, pointOfView, languageStyle, wordsPerChapter
        );

        // 去除AI味润色
        String polishedContent = stepService.polishContent(expandedContent, wordsPerChapter);

        // 字数检查和重试
        int currentWordCount = countChineseWords(polishedContent);
        int maxRetries = 3;
        int retryCount = 0;

        while (currentWordCount < wordsPerChapter && retryCount < maxRetries) {
            retryCount++;
            logger.warn("章节 '{}' 字数不足: {} < {}，第{}次重试扩写",
                chapterInfo.getSection(), currentWordCount, wordsPerChapter, retryCount);

            // 让AI继续扩写
            polishedContent = stepService.expandContent(
                polishedContent, wordsPerChapter - currentWordCount, languageStyle
            );

            // 再次润色
            polishedContent = stepService.polishContent(polishedContent, wordsPerChapter);

            currentWordCount = countChineseWords(polishedContent);
        }

        if (currentWordCount < wordsPerChapter) {
            logger.warn("章节 '{}' 经过{}次重试后字数仍不足: {} < {}",
                chapterInfo.getSection(), maxRetries, currentWordCount, wordsPerChapter);
        } else {
            logger.info("章节 '{}' 字数检查通过: {} >= {}", chapterInfo.getSection(), currentWordCount, wordsPerChapter);
        }

        return polishedContent;
    }

    /**
     * 统计中文字数（包括中文标点）
     */
    private int countChineseWords(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        // 统计中文字符和中文字标点
        int count = 0;
        for (char c : content.toCharArray()) {
            // 中文字符范围
            if (c >= 0x4E00 && c <= 0x9FFF) {
                count++;
            }
            // 中文标点
            else if (c >= 0x3000 && c <= 0x303F) {
                count++;
            }
        }
        return count;
    }

    /**
     * 保存角色卡
     */
    private void saveCharacterCards(Long novelId, List<CharacterCard> characterCards) {
        List<CharacterCard> savedCards = new ArrayList<>();
        for (int i = 0; i < characterCards.size(); i++) {
            CharacterCard card = characterCards.get(i);
            try {
                CharacterCard savedCard = characterCardService.addCharacterCard(novelId, card, i);
                savedCards.add(savedCard);
                logger.info("保存角色卡成功: {} (ID: {})", card.getName(), savedCard.getId());
            } catch (Exception e) {
                logger.error("保存角色卡失败: {}", card.getName(), e);
            }
        }

        // 异步生成角色卡的 AI 提示词和图片
        // 注意：不传递 savedCards 对象，让异步方法自己查询，避免事务可见性问题
        if (!savedCards.isEmpty()) {
            logger.info("开始异步生成 {} 个角色卡的 AI 提示词和图片", savedCards.size());
            characterCardAsyncService.processCharacterCardsAsyncByNovelId(novelId);
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

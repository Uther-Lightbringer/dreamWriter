package org.zenithon.articlecollect.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.zenithon.articlecollect.config.EvoLinkConfig;
import org.zenithon.articlecollect.dto.CharacterCard;
import org.zenithon.articlecollect.entity.CharacterCardEntity;
import org.zenithon.articlecollect.repository.CharacterCardRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

/**
 * 角色卡异步任务服务
 */
@Service
public class CharacterCardAsyncService {
    
    private static final Logger logger = LoggerFactory.getLogger(CharacterCardAsyncService.class);
    
    @Autowired
    private CharacterCardRepository characterCardRepository;
    
    @Autowired
    private CharacterCardService characterCardService;
    
    @Autowired
    private AIPromptService aiPromptService;
    
    @Autowired
    private CharacterCardPromptTaskService promptTaskService;

    @Autowired
    private EvoLinkConfig evoLinkConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SystemConfigService systemConfigService;

    // 图片生成间隔时间（毫秒）
    private static final long IMAGE_GENERATION_INTERVAL_MS = 3000; // 3 秒
    // 更新重试次数
    private static final int MAX_UPDATE_RETRIES = 5;
    // 更新重试间隔（毫秒）
    private static final long UPDATE_RETRY_INTERVAL_MS = 1000; // 1 秒
    // 查询重试次数
    private static final int MAX_QUERY_RETRIES = 10;
    // 查询重试间隔（毫秒）
    private static final long QUERY_RETRY_INTERVAL_MS = 500; // 0.5 秒

    /**
     * 异步处理角色卡的 AI 绘画提示词生成和图片生成（根据小说ID查询角色卡）
     * 推荐使用此方法，避免事务可见性问题
     * @param novelId 小说 ID
     */
    @Async("characterCardTaskExecutor")
    public void processCharacterCardsAsyncByNovelId(Long novelId) {
        logger.info("开始异步处理角色卡 AI 绘画任务，小说 ID: {}", novelId);

        // 等待主事务提交后再查询
        List<CharacterCard> characterCards = null;
        for (int attempt = 1; attempt <= MAX_QUERY_RETRIES; attempt++) {
            try {
                Thread.sleep(QUERY_RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("等待被中断", e);
                return;
            }

            try {
                characterCards = characterCardService.getCharacterCardsByNovelId(novelId);
                if (characterCards != null && !characterCards.isEmpty()) {
                    logger.info("第{}次查询成功，找到 {} 个角色卡", attempt, characterCards.size());
                    break;
                }
                logger.info("第{}次查询，角色卡列表为空，继续等待...", attempt);
            } catch (Exception e) {
                logger.warn("第{}次查询失败: {}", attempt, e.getMessage());
            }
        }

        if (characterCards == null || characterCards.isEmpty()) {
            logger.warn("小说 ID: {} 没有找到角色卡，放弃处理", novelId);
            return;
        }

        // 调用原有处理逻辑
        processCharacterCards(novelId, characterCards);
    }

    /**
     * 异步处理角色卡的 AI 绘画提示词生成和图片生成
     * @param novelId 小说 ID
     * @param characterCards 角色卡列表
     */
    @Async("characterCardTaskExecutor")
    public void processCharacterCardsAsync(Long novelId, List<CharacterCard> characterCards) {
        logger.info("开始异步处理角色卡 AI 绘画任务，小说 ID: {}, 角色数量：{}", novelId, characterCards.size());
        processCharacterCards(novelId, characterCards);
    }

    /**
     * 实际处理角色卡的逻辑
     */
    private void processCharacterCards(Long novelId, List<CharacterCard> characterCards) {
        logger.info("开始处理角色卡 AI 绘画任务，小说 ID: {}, 角色数量：{}", novelId, characterCards.size());

        for (int i = 0; i < characterCards.size(); i++) {
            CharacterCard card = characterCards.get(i);

            try {
                // 1. 如果用户没有手动填写 AI 绘画提示词，则调用 AI 生成
                if (card.getAppearanceDescription() == null || card.getAppearanceDescription().trim().isEmpty()) {
                    logger.info("正在为角色 '{}' 生成 AI 绘画提示词...", card.getName());
                    String aiPrompt = aiPromptService.generateAIPrompt(card);
                    if (aiPrompt != null && !aiPrompt.trim().isEmpty()) {
                        card.setAppearanceDescription(aiPrompt);
                        logger.info("角色 '{}' AI 绘画提示词生成成功", card.getName());
                    } else {
                        logger.warn("角色 '{}' AI 绘画提示词生成结果为空", card.getName());
                    }
                }

                // 2. 如果有 AI 绘画提示词，且没有图片 URL，则调用 EvoLink 生成图片
                if ((card.getGeneratedImageUrl() == null || card.getGeneratedImageUrl().trim().isEmpty())
                        && card.getAppearanceDescription() != null && !card.getAppearanceDescription().trim().isEmpty()) {
                    logger.info("正在为角色 '{}' 生成 AI 图片...", card.getName());

                    // 添加延迟，避免 API 并发限制
                    try {
                        Thread.sleep(IMAGE_GENERATION_INTERVAL_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.error("等待间隔被中断", e);
                    }

                    try {
                        String localImagePath = generateAndSaveCharacterImage(card);
                        if (localImagePath != null && !localImagePath.trim().isEmpty()) {
                            card.setGeneratedImageUrl(localImagePath);
                            logger.info("角色 '{}' AI 图片生成成功：{}", card.getName(), localImagePath);
                        } else {
                            logger.warn("角色 '{}' AI 图片生成结果为空", card.getName());
                        }
                    } catch (Exception e) {
                        logger.error("生成图片失败：{}", e.getMessage(), e);
                    }
                }

                // 3. 使用 CharacterCardService 更新 AI 生成的字段（带重试）
                updateWithRetry(card);

            } catch (Exception e) {
                logger.error("处理角色 '{}' 的 AI 绘画任务失败：{}", card.getName(), e.getMessage(), e);
            }
        }

        logger.info("完成所有角色卡 AI 绘画任务处理，小说 ID: {}", novelId);
    }

    /**
     * 带重试的更新方法
     */
    private void updateWithRetry(CharacterCard card) {
        for (int attempt = 1; attempt <= MAX_UPDATE_RETRIES; attempt++) {
            try {
                characterCardService.updateCharacterCardAIGeneratedFields(
                    card.getId(), card.getAppearanceDescription(), card.getGeneratedImageUrl());
                logger.info("角色卡 {} AI生成字段更新成功", card.getId());
                return;
            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().contains("角色卡不存在")) {
                    logger.warn("角色卡 {} 第{}次更新失败（角色卡不存在），等待重试...", card.getId(), attempt);
                    if (attempt < MAX_UPDATE_RETRIES) {
                        try {
                            Thread.sleep(UPDATE_RETRY_INTERVAL_MS);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                } else {
                    throw e;
                }
            }
        }
        logger.error("角色卡 {} 更新失败，已重试{}次", card.getId(), MAX_UPDATE_RETRIES);
    }
    
    /**
     * 异步重新生成单个角色卡的 AI 绘画提示词（带长轮询支持）
     * @param taskId 任务 ID
     * @param characterId 角色卡 ID
     * @param novelId 小说 ID
     */
    @Async("characterCardTaskExecutor")
    public void regenerateAIPromptAsync(String taskId, Long characterId, Long novelId) {
        logger.info("开始异步重新生成角色卡 AI 绘画提示词，taskId={}, characterId={}", taskId, characterId);
        
        try {
            // 更新任务状态为处理中
            logger.info("更新任务状态为处理中：taskId={}", taskId);
            promptTaskService.updateTaskProcessing(taskId);
            
            // 获取角色卡信息
            logger.info("获取角色卡信息：characterId={}", characterId);
            CharacterCard card = characterCardService.getCharacterCardById(characterId);
            if (card == null) {
                logger.error("角色卡不存在：characterId={}", characterId);
                promptTaskService.failTask(taskId, "角色卡不存在，ID: " + characterId);
                return;
            }
            
            logger.info("角色卡信息获取成功：name={}, 开始调用 AI 生成提示词", card.getName());
            
            // 调用 AI 生成提示词
            String aiPrompt = aiPromptService.generateAIPrompt(card);
            
            if (aiPrompt != null && !aiPrompt.trim().isEmpty()) {
                logger.info("AI 提示词生成成功，长度：{},新提示词：{}", aiPrompt.length(), aiPrompt);
                
                // 先更新实体的 appearanceDescription
                Optional<CharacterCardEntity> entityOpt = characterCardRepository.findById(characterId);
                if (entityOpt.isPresent()) {
                    CharacterCardEntity entity = entityOpt.get();
                    entity.setAppearanceDescription(aiPrompt);
                    characterCardRepository.save(entity);
                    logger.info("已更新角色卡提示词到数据库");
                }
                
                // 然后递增版本号并返回 DTO
                CharacterCard updatedCard = characterCardService.regenerateAIPrompt(characterId);
                
                logger.info("角色 '{}' AI 绘画提示词重新生成成功，版本号：{}", 
                    card.getName(), updatedCard.getPromptVersion());
                
                // 更新任务状态为完成
                promptTaskService.completeTask(taskId, updatedCard);
                logger.info("任务状态已更新为完成：taskId={}", taskId);
            } else {
                logger.warn("角色 '{}' AI 绘画提示词生成结果为空", card.getName());
                promptTaskService.failTask(taskId, "AI 提示词生成结果为空");
            }
            
        } catch (Exception e) {
            logger.error("重新生成角色卡 AI 绘画提示词失败，taskId={}, characterId={}", taskId, characterId, e);
            promptTaskService.failTask(taskId, "生成失败：" + e.getMessage());
        }
    }

    /**
     * 生成并保存角色卡图片到本地
     */
    private String generateAndSaveCharacterImage(CharacterCard card) {
        try {
            // 创建 EvoLinkImageService 实例
            EvoLinkImageService imageService = new EvoLinkImageService(evoLinkConfig, restTemplate, objectMapper, systemConfigService);

            // 为角色生成固定seed，提高角色一致性
            Integer seed = EvoLinkImageService.generateSeedForCharacter(card.getName());

            String taskId = imageService.generateImage(card.getAppearanceDescription(), "1:1", seed);

            // 等待图片生成完成
            String imageUrl = waitForImageCompletion(imageService, taskId, 5 * 60 * 1000);

            if (imageUrl != null) {
                // 下载并保存到本地
                return downloadAndSaveToLocal(card, imageUrl);
            }

            return null;

        } catch (Exception e) {
            logger.error("生成并保存角色卡图片失败：" + e.getMessage(), e);
            return null;
        }
    }

    /**
     * 下载图片并保存到本地角色卡目录
     */
    private String downloadAndSaveToLocal(CharacterCard card, String imageUrl) {
        try {
            // 下载图片
            ResponseEntity<byte[]> response = restTemplate.getForEntity(imageUrl, byte[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // 使用 FileUploadUtil 保存图片
                String localPath = org.zenithon.articlecollect.util.FileUploadUtil.saveCharacterCardImage(
                    response.getBody(),
                    card.getName()
                );

                logger.info("角色卡图片已保存到本地：{}", localPath);
                return localPath;
            }

            return null;

        } catch (Exception e) {
            logger.error("下载并保存角色卡图片失败：" + e.getMessage(), e);
            return null;
        }
    }

    /**
     * 等待图片生成完成
     */
    private String waitForImageCompletion(EvoLinkImageService imageService, String taskId, long timeout) {
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeout) {
            try {
                EvoLinkImageService.TaskStatus status = imageService.getTaskStatus(taskId);

                if (status.isCompleted()) {
                    return status.getImageUrl();
                }

                if (status.isFailed()) {
                    logger.error("图片生成失败：{}", status.getError());
                    return null;
                }

                // 等待 2 秒后再次检查
                Thread.sleep(2000);

            } catch (Exception e) {
                logger.error("检查任务状态失败：" + e.getMessage(), e);
                return null;
            }
        }

        logger.error("等待图片生成超时");
        return null;
    }
}

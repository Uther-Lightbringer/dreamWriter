package org.zenithon.articlecollect.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zenithon.articlecollect.dto.CharacterCard;
import org.zenithon.articlecollect.entity.CharacterCardEntity;
import org.zenithon.articlecollect.repository.CharacterCardRepository;

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
    
    // 图片生成间隔时间（毫秒）
    private static final long IMAGE_GENERATION_INTERVAL_MS = 3000; // 3 秒
    
    /**
     * 异步处理角色卡的 AI 绘画提示词生成和图片生成
     * @param novelId 小说 ID
     * @param characterCards 角色卡列表
     */
    @Async("characterCardTaskExecutor")
    @Transactional
    public void processCharacterCardsAsync(Long novelId, List<CharacterCard> characterCards) {
        logger.info("开始异步处理角色卡 AI 绘画任务，小说 ID: {}, 角色数量：{}", novelId, characterCards.size());
        
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
                
                // 2. 如果有 AI 绘画提示词，且没有图片 URL，则调用火山引擎生成图片
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
                        String imageUrl = new VolcEngineImageService().generateImage(card.getAppearanceDescription());
                        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                            card.setGeneratedImageUrl(imageUrl);
                            logger.info("角色 '{}' AI 图片生成成功：{}", card.getName(), imageUrl);
                        } else {
                            logger.warn("角色 '{}' AI 图片生成结果为空", card.getName());
                        }
                    } catch (Exception e) {
                        logger.error("生成图片失败：{}", e.getMessage(), e);
                    }
                }
                
                // 3. 使用 CharacterCardService 更新 AI 生成的字段
                characterCardService.updateCharacterCardAIGeneratedFields(
                    card.getId(), card.getAppearanceDescription(), card.getGeneratedImageUrl());
                
            } catch (Exception e) {
                logger.error("处理角色 '{}' 的 AI 绘画任务失败：{}", card.getName(), e.getMessage(), e);
            }
        }
        
        logger.info("完成所有角色卡 AI 绘画任务处理，小说 ID: {}", novelId);
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
}

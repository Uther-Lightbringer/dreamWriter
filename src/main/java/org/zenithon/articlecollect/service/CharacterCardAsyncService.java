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

/**
 * 角色卡异步任务服务
 */
@Service
public class CharacterCardAsyncService {
    
    private static final Logger logger = LoggerFactory.getLogger(CharacterCardAsyncService.class);
    
    @Autowired
    private CharacterCardRepository characterCardRepository;
    
    @Autowired
    private AIPromptService aiPromptService;
    
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
                    
                    String imageUrl = new VolcEngineImageService().generateImage(card.getAppearanceDescription());
                    if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                        card.setGeneratedImageUrl(imageUrl);
                        logger.info("角色 '{}' AI 图片生成成功：{}", card.getName(), imageUrl);
                    } else {
                        logger.warn("角色 '{}' AI 图片生成结果为空", card.getName());
                    }
                }
                
                // 3. 更新数据库中的角色卡数据（包含生成的提示词和图片 URL）
                updateCharacterCardInDatabase(card, novelId, i);
                
            } catch (Exception e) {
                logger.error("处理角色 '{}' 的 AI 绘画任务失败：{}", card.getName(), e.getMessage(), e);
            }
        }
        
        logger.info("完成所有角色卡 AI 绘画任务处理，小说 ID: {}", novelId);
    }
    
    /**
     * 更新单个角色卡在数据库中的数据
     */
    private void updateCharacterCardInDatabase(CharacterCard card, Long novelId, int sortOrder) {
        try {
            // 查找已存在的角色卡
            CharacterCardEntity existingEntity = characterCardRepository.findByNovelIdAndCharacterIdOrNull(
                novelId, card.getId());
            
            if (existingEntity != null) {
                // 更新现有记录
                existingEntity.setAppearanceDescription(card.getAppearanceDescription());
                existingEntity.setGeneratedImageUrl(card.getGeneratedImageUrl());
                existingEntity.setUpdateTime(java.time.LocalDateTime.now());
                characterCardRepository.save(existingEntity);
                logger.info("角色卡 '{}' 数据库记录更新成功", card.getName());
            } else {
                // 如果不存在，创建新记录（正常情况下应该不会发生）
                logger.warn("未找到角色卡 '{}' 的现有记录，创建新记录", card.getName());
                CharacterCardEntity entity = convertToEntity(card, novelId, sortOrder);
                characterCardRepository.save(entity);
            }
        } catch (Exception e) {
            logger.error("更新角色卡 '{}' 数据库记录失败：{}", card.getName(), e.getMessage(), e);
            throw new RuntimeException("更新角色卡数据库记录失败", e);
        }
    }
    
    /**
     * 将 DTO 转换为实体（复用 NovelService 的逻辑）
     */
    private CharacterCardEntity convertToEntity(CharacterCard card, Long novelId, int sortOrder) {
        CharacterCardEntity entity = new CharacterCardEntity();
        entity.setCharacterId(card.getId());
        entity.setName(card.getName());
        entity.setNovelId(novelId);
        entity.setSortOrder(sortOrder);
        entity.setAge(card.getAge());
        entity.setGender(card.getGender());
        entity.setOccupation(card.getOccupation());
        entity.setPersonality(card.getPersonality());
        entity.setBackground(card.getBackground());
        entity.setNotes(card.getNotes());
        entity.setAppearanceDescription(card.getAppearanceDescription());
        entity.setGeneratedImageUrl(card.getGeneratedImageUrl());
        
        // 其他字段省略，因为主要目的是更新 AI 生成的字段
        
        return entity;
    }
}

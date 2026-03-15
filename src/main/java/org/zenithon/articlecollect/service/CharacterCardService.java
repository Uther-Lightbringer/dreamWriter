package org.zenithon.articlecollect.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zenithon.articlecollect.dto.CharacterCard;
import org.zenithon.articlecollect.dto.CharacterCardAppearance;
import org.zenithon.articlecollect.dto.CharacterCardRelationship;
import org.zenithon.articlecollect.entity.CharacterCardEntity;
import org.zenithon.articlecollect.repository.CharacterCardRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 角色卡业务服务类
 */
@Service
public class CharacterCardService {
    
    private static final Logger logger = LoggerFactory.getLogger(CharacterCardService.class);
    
    @Autowired
    private CharacterCardRepository characterCardRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AIPromptService aiPromptService;
    
    /**
     * 根据小说 ID 获取所有角色卡（按排序顺序）
     */
    public List<CharacterCard> getCharacterCardsByNovelId(Long novelId) {
        List<CharacterCardEntity> entities = characterCardRepository.findByNovelIdOrderBySortOrderAsc(novelId);
        List<CharacterCard> cards = new ArrayList<>();
        
        for (CharacterCardEntity entity : entities) {
            CharacterCard card = convertToDTO(entity);
            cards.add(card);
        }
        
        return cards;
    }

    
    /**
     * 检查角色是否存在
     */
    public boolean existsCharacterCard(Long characterId) {
        return characterCardRepository.existsById(characterId);
    }
    
    /**
     * 保存角色卡列表
     */
    @Transactional
    public List<CharacterCard> saveCharacterCards(Long novelId, List<CharacterCard> characterCards) {

        List<CharacterCard> entities = new ArrayList<>();
        // 保存新的角色卡
        for (int i = 0; i < characterCards.size(); i++) {
            CharacterCard card = characterCards.get(i);
            CharacterCardEntity entity = convertToEntity(card, novelId, i);
            CharacterCardEntity saved = characterCardRepository.save(entity);
            CharacterCard newCard = convertToDTO(saved);
            entities.add(newCard);
        }
        logger.info("保存了 {} 个角色卡到数据库，小说 ID: {}", characterCards.size(), novelId);
        return entities;
    }
    
    /**
     * 保存单个角色卡（新增或更新）
     * 如果提供了 ID 则更新，否则创建新角色卡
     */
    @Transactional
    public CharacterCard saveSingleCharacterCard(Long novelId, CharacterCard characterCard) {
        logger.info("保存单个角色卡到小说 ID: {}, 角色名：{}", novelId, characterCard.getName());
        
        // 如果有 ID，尝试更新现有角色卡
        if (characterCard.getId() != null) {
            Optional<CharacterCardEntity> existingOpt = characterCardRepository.findById(characterCard.getId());
            if (existingOpt.isPresent()) {
                // 验证角色卡是否属于该小说
                CharacterCardEntity existingEntity = existingOpt.get();
                if (!existingEntity.getNovelId().equals(novelId)) {
                    throw new RuntimeException("角色卡不属于该小说，ID: " + characterCard.getId());
                }
                
                // 更新字段
                updateEntityFields(existingEntity, characterCard);
                CharacterCardEntity savedEntity = characterCardRepository.save(existingEntity);
                logger.info("更新角色卡：{} (ID: {})", characterCard.getName(), characterCard.getId());
                return convertToDTO(savedEntity);
            }
        }
        
        // 创建新角色卡
        int sortOrder = characterCardRepository.findByNovelIdOrderBySortOrderAsc(novelId).size();
        CharacterCardEntity newEntity = convertToEntity(characterCard, novelId, sortOrder);
        CharacterCardEntity savedEntity = characterCardRepository.save(newEntity);
        logger.info("创建新角色卡：{} (ID: {})", characterCard.getName(), savedEntity.getId());
        
        return convertToDTO(savedEntity);
    }
    
    /**
     * 添加单个角色卡
     */
    @Transactional
    public CharacterCard addCharacterCard(Long novelId, CharacterCard characterCard, int sortOrder) {
        CharacterCardEntity entity = convertToEntity(characterCard, novelId, sortOrder);
        CharacterCardEntity savedEntity = characterCardRepository.save(entity);
        
        logger.info("添加角色卡：{} (ID: {}) 到小说 ID: {}", characterCard.getName(), characterCard.getId(), novelId);
        
        return convertToDTO(savedEntity);
    }
    
    /**
     * 更新单个角色卡
     */
    @Transactional
    public CharacterCard updateCharacterCard(Long characterId, CharacterCard updatedCard) {
        Optional<CharacterCardEntity> entityOpt = characterCardRepository.findById(characterId);
        
        if (entityOpt.isPresent()) {
            CharacterCardEntity existingEntity = entityOpt.get();
            
            // 更新字段
            updateEntityFields(existingEntity, updatedCard);
            
            CharacterCardEntity savedEntity = characterCardRepository.save(existingEntity);
            logger.info("更新角色卡：{} (ID: {})", updatedCard.getName(), characterId);
            
            return convertToDTO(savedEntity);
        }
        
        throw new RuntimeException("角色卡不存在，ID: " + characterId);
    }
    
    /**
     * 删除单个角色卡
     */
    @Transactional
    public void deleteCharacterCard(Long characterId) {
        Optional<CharacterCardEntity> entityOpt = characterCardRepository.findById(characterId);
        
        if (entityOpt.isPresent()) {
            characterCardRepository.delete(entityOpt.get());
            logger.info("删除角色卡：{} (ID: {})", entityOpt.get().getName(), characterId);
        } else {
            throw new RuntimeException("角色卡不存在，ID: " + characterId);
        }
    }
    
    /**
     * 批量删除角色卡
     */
    @Transactional
    public void deleteAllCharacterCards(Long novelId) {
        characterCardRepository.deleteByNovelId(novelId);
        logger.info("删除小说 ID: {} 的所有角色卡", novelId);
    }
    
    /**
     * 更新角色卡的 AI 绘画提示词（带版本号）
     * 注意：此方法假设 appearanceDescription 已经被设置好
     */
    @Transactional
    public CharacterCard regenerateAIPrompt(Long characterId) {
        Optional<CharacterCardEntity> entityOpt = characterCardRepository.findById(characterId);
        
        if (entityOpt.isPresent()) {
            CharacterCardEntity existingEntity = entityOpt.get();
            
            // 递增提示词版本号
            Integer currentVersion = existingEntity.getPromptVersion();
            existingEntity.setPromptVersion(currentVersion == null ? 1 : currentVersion + 1);
            existingEntity.setUpdateTime(java.time.LocalDateTime.now());
            
            CharacterCardEntity savedEntity = characterCardRepository.save(existingEntity);
            logger.info("重新生成角色卡 {} 的 AI 绘画提示词，版本号：{}", characterId, savedEntity.getPromptVersion());
            
            return convertToDTO(savedEntity);
        }
        
        throw new RuntimeException("角色卡不存在，ID: " + characterId);
    }
    
    /**
     * 更新角色卡的 AI 绘画提示词和图片 URL（带版本号）
     */
    @Transactional
    public CharacterCard regenerateAIImage(Long characterId, String appearanceDescription, String generatedImageUrl) {
        Optional<CharacterCardEntity> entityOpt = characterCardRepository.findById(characterId);
        
        if (entityOpt.isPresent()) {
            CharacterCardEntity existingEntity = entityOpt.get();
            
            // 如果有新的描述，先更新描述
            if (appearanceDescription != null && !appearanceDescription.trim().isEmpty()) {
                existingEntity.setAppearanceDescription(appearanceDescription);
            }
            
            // 更新图片 URL
            existingEntity.setGeneratedImageUrl(generatedImageUrl);
            
            // 递增图片版本号
            Integer currentVersion = existingEntity.getImageVersion();
            existingEntity.setImageVersion(currentVersion == null ? 1 : currentVersion + 1);
            existingEntity.setUpdateTime(java.time.LocalDateTime.now());
            
            CharacterCardEntity savedEntity = characterCardRepository.save(existingEntity);
            logger.info("重新生成角色卡 {} 的 AI 图片，版本号：{}", characterId, savedEntity.getImageVersion());
            
            return convertToDTO(savedEntity);
        }
        
        throw new RuntimeException("角色卡不存在，ID: " + characterId);
    }
    
    /**
     * 更新角色卡的 AI 绘画提示词和图片 URL
     */
    @Transactional
    public void updateCharacterCardAIGeneratedFields(Long characterId, String appearanceDescription, String generatedImageUrl) {
        Optional<CharacterCardEntity> entityOpt = characterCardRepository.findById(characterId);
        
        if (entityOpt.isPresent()) {
            CharacterCardEntity existingEntity = entityOpt.get();
            existingEntity.setAppearanceDescription(appearanceDescription);
            existingEntity.setGeneratedImageUrl(generatedImageUrl);
            existingEntity.setUpdateTime(java.time.LocalDateTime.now());
            
            characterCardRepository.save(existingEntity);
            logger.info("更新角色卡 {} 的 AI 生成字段", characterId);
        } else {
            throw new RuntimeException("角色卡不存在，ID: " + characterId);
        }
    }
    
    /**
     * 将 DTO 转换为实体
     */
    private CharacterCardEntity convertToEntity(CharacterCard card, Long novelId, int sortOrder) {
        CharacterCardEntity entity = new CharacterCardEntity();
        entity.setName(card.getName());
        entity.setNovelId(novelId);
        entity.setSortOrder(sortOrder);
        
        // 处理别名数组
        if (card.getAlternativeNames() != null && !card.getAlternativeNames().isEmpty()) {
            try {
                entity.setAlternativeNames(objectMapper.writeValueAsString(card.getAlternativeNames()));
            } catch (JsonProcessingException e) {
                entity.setAlternativeNames("[]");
            }
        }
        
        // 处理外貌特征对象
        if (card.getAppearance() != null) {
            try {
                entity.setAppearanceJson(objectMapper.writeValueAsString(card.getAppearance()));
            } catch (JsonProcessingException e) {
                entity.setAppearanceJson("{}");
            }
        }
        
        // 处理关系数组
        if (card.getRelationships() != null && !card.getRelationships().isEmpty()) {
            try {
                entity.setRelationshipsJson(objectMapper.writeValueAsString(card.getRelationships()));
            } catch (JsonProcessingException e) {
                entity.setRelationshipsJson("[]");
            }
        }
        
        entity.setAge(card.getAge());
        entity.setGender(card.getGender());
        entity.setOccupation(card.getOccupation());
        entity.setAppearanceDescription(card.getAppearanceDescription());
        entity.setPersonality(card.getPersonality());
        entity.setBackground(card.getBackground());
        entity.setNotes(card.getNotes());
        entity.setGeneratedImageUrl(card.getGeneratedImageUrl());
        entity.setPromptVersion(card.getPromptVersion());
        entity.setImageVersion(card.getImageVersion());
        
        return entity;
    }
    
    /**
     * 将实体转换为 DTO
     */
    private CharacterCard convertToDTO(CharacterCardEntity entity) {
        CharacterCard card = new CharacterCard();
        card.setId(entity.getId());
        card.setName(entity.getName());
        card.setAge(entity.getAge());
        card.setGender(entity.getGender());
        card.setOccupation(entity.getOccupation());
        card.setPersonality(entity.getPersonality());
        card.setBackground(entity.getBackground());
        card.setNotes(entity.getNotes());
        card.setAppearanceDescription(entity.getAppearanceDescription());
        card.setGeneratedImageUrl(entity.getGeneratedImageUrl());
        card.setPromptVersion(entity.getPromptVersion());
        card.setImageVersion(entity.getImageVersion());
        
        // 解析别名字符串
        if (entity.getAlternativeNames() != null && !entity.getAlternativeNames().isEmpty()) {
            try {
                List<String> names = objectMapper.readValue(
                    entity.getAlternativeNames(), 
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}
                );
                card.setAlternativeNames(names);
            } catch (JsonProcessingException e) {
                card.setAlternativeNames(new ArrayList<>());
            }
        }

        // 解析外貌特征字符串
        if (entity.getAppearanceJson() != null && !entity.getAppearanceJson().isEmpty()) {
            try {
                CharacterCardAppearance appearance = objectMapper.readValue(
                    entity.getAppearanceJson(),
                    CharacterCardAppearance.class
                );
                card.setAppearance(appearance);
            } catch (JsonProcessingException e) {
                card.setAppearance(null);
            }
        }
        
        // 解析关系字符串
        if (entity.getRelationshipsJson() != null && !entity.getRelationshipsJson().isEmpty()) {
            try {
                List<CharacterCardRelationship> relationships = objectMapper.readValue(
                    entity.getRelationshipsJson(), 
                    new com.fasterxml.jackson.core.type.TypeReference<List<CharacterCardRelationship>>() {}
                );
                card.setRelationships(relationships);
            } catch (JsonProcessingException e) {
                card.setRelationships(new ArrayList<>());
            }
        }
        
        return card;
    }
    
    /**
     * 更新实体字段（用于部分更新）
     */
    private void updateEntityFields(CharacterCardEntity entity, CharacterCard updatedCard) {
        entity.setName(updatedCard.getName());
        entity.setAge(updatedCard.getAge());
        entity.setGender(updatedCard.getGender());
        entity.setOccupation(updatedCard.getOccupation());
        entity.setPersonality(updatedCard.getPersonality());
        entity.setBackground(updatedCard.getBackground());
        entity.setNotes(updatedCard.getNotes());
        
        // 如果有新的 AI 绘画提示词或图片 URL，也更新
        if (updatedCard.getAppearanceDescription() != null) {
            entity.setAppearanceDescription(updatedCard.getAppearanceDescription());
        }
        if (updatedCard.getGeneratedImageUrl() != null) {
            entity.setGeneratedImageUrl(updatedCard.getGeneratedImageUrl());
        }
        
        // 处理别名数组
        if (updatedCard.getAlternativeNames() != null) {
            try {
                entity.setAlternativeNames(objectMapper.writeValueAsString(updatedCard.getAlternativeNames()));
            } catch (JsonProcessingException e) {
                entity.setAlternativeNames("[]");
            }
        }
        
        // 处理外貌特征对象
        if (updatedCard.getAppearance() != null) {
            try {
                entity.setAppearanceJson(objectMapper.writeValueAsString(updatedCard.getAppearance()));
            } catch (JsonProcessingException e) {
                entity.setAppearanceJson("{}");
            }
        }
        
        // 处理关系数组
        if (updatedCard.getRelationships() != null) {
            try {
                entity.setRelationshipsJson(objectMapper.writeValueAsString(updatedCard.getRelationships()));
            } catch (JsonProcessingException e) {
                entity.setRelationshipsJson("[]");
            }
        }
        
        entity.setUpdateTime(java.time.LocalDateTime.now());
    }

    /**
     * 根据角色 ID 获取单个角色卡
     */
    public CharacterCard getCharacterCardById(Long characterId) {
        return convertToDTO(Objects.requireNonNull(characterCardRepository.findById(characterId).orElse(null)));
    }
}

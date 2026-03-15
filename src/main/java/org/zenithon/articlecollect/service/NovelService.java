package org.zenithon.articlecollect.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zenithon.articlecollect.dto.ChapterWithTags;
import org.zenithon.articlecollect.dto.CharacterCard;
import org.zenithon.articlecollect.dto.CharacterCardAppearance;
import org.zenithon.articlecollect.dto.CharacterCardRelationship;
import org.zenithon.articlecollect.entity.ChapterTag;
import org.zenithon.articlecollect.entity.Novel;
import org.zenithon.articlecollect.entity.Chapter;
import org.zenithon.articlecollect.entity.ChapterDetailView;
import org.zenithon.articlecollect.entity.CharacterCardEntity;
import org.zenithon.articlecollect.repository.NovelRepository;
import org.zenithon.articlecollect.repository.ChapterRepository;
import org.zenithon.articlecollect.repository.ChapterTagRepository;
import org.zenithon.articlecollect.repository.CharacterCardRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 小说管理服务类
 */
@Service
public class NovelService {
    
    private static final Logger logger = LoggerFactory.getLogger(NovelService.class);
    
    @Autowired
    private NovelRepository novelRepository;
    
    @Autowired
    private ChapterRepository chapterRepository;
    
    @Autowired
    private ChapterTagRepository chapterTagRepository;
    
    @Autowired
    private ChapterTagService chapterTagService;
    
    @Autowired
    private CharacterCardRepository characterCardRepository;
    
    @Autowired
    private AIPromptService aiPromptService;
    
    @Autowired
    private CharacterCardAsyncService characterCardAsyncService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 创建新小说
     */
    public Novel createNovel(String title) {
        Novel novel = new Novel(title);
        return novelRepository.save(novel);
    }
    
    /**
     * 创建新小说（带作者信息）
     */
    public Novel createNovel(String title, String author) {
        Novel novel = new Novel(title, author);
        return novelRepository.save(novel);
    }
    
    /**
     * 创建新小说（完整信息）
     */
    public Novel createNovel(String title, String author, String description) {
        Novel novel = new Novel(title, author, description);
        return novelRepository.save(novel);
    }
    
    /**
     * 根据ID获取小说
     */
    public Novel getNovelById(Long novelId) {
        Optional<Novel> novelOpt = novelRepository.findById(novelId);
        if (novelOpt.isPresent()) {
            Novel novel = novelOpt.get();
            // 确保格式化时间字段被正确设置
             ensureFormattedTime(novel);
            setChaptersCount(novel);
            // 批量分析章节标签（首次访问时生成，后续直接读取数据库）
            chapterTagService.batchAnalyzeNovelChapters(novelId);
            
            // 对章节进行排序并重新设置
            List<Chapter> sortedChapters = novel.getChapters();
            if (sortedChapters != null) {
                Collections.sort(sortedChapters, Comparator.comparing(Chapter::getChapterNumber));
                novel.setChapters(sortedChapters);
            }
            
            // 设置章节数
            setChaptersCount(novel);
            
            return novel;
        }
        return null;
    }

    private void setChaptersCount(Novel novel) {
        if (novel.getChapters() != null) {
            novel.setChaptersCount(novel.getChapters().size());
        } else {
            novel.setChaptersCount(0);
        }
    }

    /**
     * 获取所有小说，按创建时间倒序排列（最新的在最前面）
     */
    public List<Novel> getAllNovels() {
        List<Novel> novels = novelRepository.findAllByOrderByCreateTimeDesc();
        // 为所有小说确保格式化时间字段被正确设置
        novels.forEach(n -> n.setChaptersCount(n.getChapters().size()));
        novels.forEach(this::ensureFormattedTime);
        return novels;
    }
    
    /**
     * 确保小说对象的格式化时间字段被正确设置
     */
    private void ensureFormattedTime(Novel novel) {
        if (novel.getCreateTime() != null && novel.getFormattedCreateTime() == null) {
            novel.setFormattedCreateTime(novel.formatDateTime(novel.getCreateTime()));
        }
    }
    
    /**
     * 删除小说
     */
    public boolean deleteNovel(Long novelId) {
        if (novelRepository.existsById(novelId)) {
            // 先删除相关章节
            List<Chapter> chaptersToDelete = chapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId);
            chapterRepository.deleteAll(chaptersToDelete);
            
            // 删除相关角色卡
            characterCardRepository.deleteByNovelId(novelId);
            
            // 删除小说
            novelRepository.deleteById(novelId);
            return true;
        }
        return false;
    }
    
    /**
     * 创建小说章节
     */
    public Chapter createChapter(Long novelId, String title, String content, Long index, String storySummary) {
        // 检查小说是否存在
        if (!novelRepository.existsById(novelId)) {
            throw new RuntimeException("小说不存在，ID: " + novelId);
        }
        int chapterNumber = 1;
        if (Objects.isNull(index)){
            chapterNumber = chapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId).size() + 1;
        }else {
            chapterNumber = index.intValue() + 1;
        }
        Chapter chapter = new Chapter(novelId, title, content, chapterNumber, storySummary);
        return chapterRepository.save(chapter);
    }
    
    /**
     * 根据小说ID获取所有章节
     */
    public List<Chapter> getChaptersByNovelId(Long novelId) {
        List<Chapter> chapters = chapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId);
        // 为所有章节确保格式化时间字段被正确设置
        chapters.forEach(this::ensureFormattedTime);
        return chapters;
    }
    
    /**
     * 根据小说ID获取所有带标签的章节
     */
    public List<ChapterWithTags> getChaptersWithTagsByNovelId(Long novelId) {
        List<Chapter> chapters = getChaptersByNovelId(novelId);
        Map<Long, List<ChapterTag>> chapterTagsMap = chapterTagService.batchAnalyzeNovelChapters(novelId);
        
        List<ChapterWithTags> result = new ArrayList<>();
        for (Chapter chapter : chapters) {
            List<ChapterTag> tags = chapterTagsMap.getOrDefault(chapter.getId(), new ArrayList<>());
            result.add(new ChapterWithTags(chapter, tags));
        }
        
        return result;
    }
    
    /**
     * 根据ID获取章节
     */
    public Chapter getChapterById(Long chapterId) {
        Optional<Chapter> chapterOpt = chapterRepository.findById(chapterId);
        if (chapterOpt.isPresent()) {
            Chapter chapter = chapterOpt.get();
            // 确保格式化时间字段被正确设置
            ensureFormattedTime(chapter);
            return chapter;
        }
        return null;
    }
    
    /**
     * 更新章节
     */
    public Chapter updateChapter(Long chapterId, String title, String content) {
        Optional<Chapter> chapterOpt = chapterRepository.findById(chapterId);
        if (chapterOpt.isPresent()) {
            Chapter chapter = chapterOpt.get();
            chapter.setTitle(title);
            chapter.setContent(content);
            chapter.setUpdateTime(LocalDateTime.now());
            return chapterRepository.save(chapter);
        }
        return null;
    }
    
    
    /**
     * 删除章节
     */
    public boolean deleteChapter(Long chapterId) {
        if (chapterRepository.existsById(chapterId)) {
            chapterRepository.deleteById(chapterId);
            return true;
        }
        return false;
    }
    
    /**
     * 获取章节详情（包含导航信息）
     */
    public ChapterDetailView getChapterDetailView(Long novelId, Long chapterId) {
        // 验证小说和章节是否存在
        Novel novel = getNovelById(novelId);
        Chapter chapter = getChapterById(chapterId);
        
        if (novel == null || chapter == null || !chapter.getNovelId().equals(novelId)) {
            return null;
        }
        
        // 获取该小说的所有章节并排序
        List<Chapter> novelChapters = getChaptersByNovelId(novelId);
        
        // 找到当前章节的索引
        int currentIndex = -1;
        for (int i = 0; i < novelChapters.size(); i++) {
            if (novelChapters.get(i).getId().equals(chapterId)) {
                currentIndex = i;
                break;
            }
        }
        
        if (currentIndex == -1) {
            return null;
        }
        
        // 计算前后章节信息
        boolean hasPrevious = currentIndex > 0;
        boolean hasNext = currentIndex < novelChapters.size() - 1;
        
        Long previousChapterId = hasPrevious ? novelChapters.get(currentIndex - 1).getId() : null;
        Long nextChapterId = hasNext ? novelChapters.get(currentIndex + 1).getId() : null;
        
        return new ChapterDetailView(
            chapter,
            hasNext,
            hasPrevious,
            nextChapterId,
            previousChapterId,
            novel.getTitle(),
            novelId
        );
    }
    
    /**
     * 确保章节对象的格式化时间字段被正确设置
     */
    private void ensureFormattedTime(Chapter chapter) {
        if (chapter.getCreateTime() != null && chapter.getFormattedCreateTime() == null) {
            chapter.setFormattedCreateTime(chapter.formatDateTime(chapter.getCreateTime()));
        }
    }
    
    /**
     * 更新小说的世界观
     */
    public Novel updateWorldView(Long novelId, String worldView) {
        Optional<Novel> novelOpt = novelRepository.findById(novelId);
        if (novelOpt.isPresent()) {
            Novel novel = novelOpt.get();
            novel.setWorldView(worldView);
            return novelRepository.save(novel);
        }
        throw new RuntimeException("小说不存在，ID: " + novelId);
    }
    
    /**
     * 更新小说的角色卡
     */
    public Novel updateCharacterCards(Long novelId, String characterCards) {
        Optional<Novel> novelOpt = novelRepository.findById(novelId);
        if (novelOpt.isPresent()) {
            Novel novel = novelOpt.get();
            novel.setCharacterCards(characterCards);
            return novelRepository.save(novel);
        }
        throw new RuntimeException("小说不存在，ID: " + novelId);
    }
    
    /**
     * 批量更新小说的角色卡（结构化数据）- 保存到数据库表
     */
    @Transactional
    public Novel updateCharacterCardsBatch(Long novelId, List<CharacterCard> characterCards) {
        Optional<Novel> novelOpt = novelRepository.findById(novelId);
        if (novelOpt.isPresent()) {
            try {
                Novel novel = novelOpt.get();
                
                // 1. 保存到数据库表
                saveCharacterCardsToDatabase(novelId, characterCards);
                
                // 2. 同时保留原有的 JSON 格式作为备份
                objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
                String json = objectMapper.writeValueAsString(characterCards);
                novel.setCharacterCards(json);
                return novelRepository.save(novel);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("角色卡数据序列化失败：" + e.getMessage());
            }
        }
        throw new RuntimeException("小说不存在，ID: " + novelId);
    }
    
    /**
     * 更新单个角色信息（结构化数据）- 保存到数据库表
     * @param novelId 小说 ID
     * @param characterCard 角色卡信息
     * @return 更新后的小说对象
     */
    @Transactional
    public Novel updateSingleCharacterCard(Long novelId, CharacterCard characterCard) {
        Optional<Novel> novelOpt = novelRepository.findById(novelId);
        if (novelOpt.isPresent()) {
            try {
                Novel novel = novelOpt.get();

                // 1. 获取现有的所有角色卡
                List<CharacterCard> existingCards = getCharacterCardsList(novelId);

                // 2. 找到要更新的角色位置并替换
                boolean found = false;
                for (int i = 0; i < existingCards.size(); i++) {
                    if (existingCards.get(i).getId().equals(characterCard.getId())) {
                        existingCards.set(i, characterCard);
                        found = true;
                        break;
                    }
                }

                // 3. 如果没有找到，则添加到列表末尾
                if (!found) {
                    existingCards.add(characterCard);
                }

                // 4. 保存到数据库表
                saveCharacterCardsToDatabase(novelId, existingCards);

                // 5. 同时保留原有的 JSON 格式作为备份
                objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
                String json = objectMapper.writeValueAsString(existingCards);
                novel.setCharacterCards(json);
                return novelRepository.save(novel);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("角色卡数据序列化失败：" + e.getMessage());
            }
        }
        throw new RuntimeException("小说不存在，ID: " + novelId);
    }
    
    /**
     * 保存角色卡到数据库表（仅保存基础数据，不包含 AI 生成）
     */
    @Transactional
    public void saveCharacterCardsToDatabase(Long novelId, List<CharacterCard> characterCards) {
        // 删除旧的角色卡数据
        characterCardRepository.deleteByNovelId(novelId);
        
        // 保存新的角色卡（不包含 AI 绘画提示词和图片生成）
        for (int i = 0; i < characterCards.size(); i++) {
            CharacterCard card = characterCards.get(i);
            CharacterCardEntity entity = convertToEntity(card, novelId, i);
            characterCardRepository.save(entity);
        }

    }
    
    /**
     * 从数据库获取角色卡列表
     */
    public List<CharacterCard> getCharacterCardsFromDatabase(Long novelId) {
        List<CharacterCardEntity> entities = characterCardRepository.findByNovelIdOrderBySortOrderAsc(novelId);
        List<CharacterCard> cards = new ArrayList<>();
        
        for (CharacterCardEntity entity : entities) {
            CharacterCard card = convertToDTO(entity);
            cards.add(card);
        }
        
        return cards;
    }
    
    /**
     * 将 DTO 转换为实体
     */
    private CharacterCardEntity convertToEntity(CharacterCard card, Long novelId, int sortOrder) {
        CharacterCardEntity entity = new CharacterCardEntity();
        entity.setCharacterId(card.getId());
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
        entity.setAppearanceJson(card.getAppearance() != null ? card.getAppearance().toString() : null);
        entity.setAppearanceDescription(card.getAppearanceDescription());
        entity.setPersonality(card.getPersonality());
        entity.setBackground(card.getBackground());
        entity.setNotes(card.getNotes());
        
        return entity;
    }
    
    /**
     * 将实体转换为 DTO
     */
    private CharacterCard convertToDTO(CharacterCardEntity entity) {
        CharacterCard card = new CharacterCard();
        card.setId(entity.getCharacterId());
        card.setName(entity.getName());
        card.setAge(entity.getAge());
        card.setGender(entity.getGender());
        card.setOccupation(entity.getOccupation());
        card.setPersonality(entity.getPersonality());
        card.setBackground(entity.getBackground());
        card.setNotes(entity.getNotes());
        card.setAppearanceDescription(entity.getAppearanceDescription());
        card.setGeneratedImageUrl(entity.getGeneratedImageUrl()); // 添加图片 URL 字段
        
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
     * 获取小说的角色卡列表（结构化数据）- 优先从数据库读取
     */
    public List<CharacterCard> getCharacterCardsList(Long novelId) {
        // 首先尝试从数据库表读取
        try {
            List<CharacterCard> cards = getCharacterCardsFromDatabase(novelId);
            if (cards != null && !cards.isEmpty()) {
                return cards;
            }
        } catch (Exception e) {
            System.err.println("从数据库读取角色卡失败：" + e.getMessage());
        }
        
        // 如果数据库表没有数据，则从 JSON 字段读取（向后兼容）
        Optional<Novel> novelOpt = novelRepository.findById(novelId);
        if (novelOpt.isPresent()) {
            Novel novel = novelOpt.get();
            String characterCardsJson = novel.getCharacterCards();
            
            if (characterCardsJson == null || characterCardsJson.trim().isEmpty()) {
                return new ArrayList<>();
            }
            
            try {
                // 尝试解析为角色卡数组
                CharacterCard[] cards = objectMapper.readValue(characterCardsJson, CharacterCard[].class);
                return Arrays.asList(cards);
            } catch (JsonProcessingException e) {
                // 如果不是有效的 JSON 或格式不匹配，返回空列表
                System.err.println("解析角色卡失败：" + e.getMessage());
                return new ArrayList<>();
            }
        }
        throw new RuntimeException("小说不存在，ID: " + novelId);
    }
    
    /**
     * 为单个角色生成 AI 绘画提示词
     */
    public String generateAIPromptForCharacter(CharacterCard characterCard) {
        return aiPromptService.generateAIPrompt(characterCard);
    }
    
    /**
     * 为单个角色生成图片
     */
    public String generateImageForCharacter(CharacterCard characterCard) throws Exception {
        return new VolcEngineImageService().generateImage(characterCard.getAppearanceDescription());
    }
}
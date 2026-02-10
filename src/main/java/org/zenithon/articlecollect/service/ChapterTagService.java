package org.zenithon.articlecollect.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zenithon.articlecollect.entity.Chapter;
import org.zenithon.articlecollect.entity.ChapterTag;
import org.zenithon.articlecollect.repository.ChapterRepository;
import org.zenithon.articlecollect.repository.ChapterTagRepository;

import java.util.*;

/**
 * 章节标签服务类
 * 负责章节内容分析和标签生成
 */
@Service
public class ChapterTagService {
    
    @Autowired
    private ChapterTagRepository chapterTagRepository;
    
    @Autowired
    private ChapterRepository chapterRepository;
    
    // 定义关键词映射
    private static final Map<String, List<String>> TAG_KEYWORDS = new HashMap<>();
    
    static {
        // 惩罚相关关键词
        TAG_KEYWORDS.put("punishment", Arrays.asList(
            "惩罚小雪"
        ));
        
        // 奖励相关关键词
        TAG_KEYWORDS.put("reward", Arrays.asList(
            "奖励小雪"
        ));
        
        // 训练相关关键词
        TAG_KEYWORDS.put("training", Arrays.asList(
            "训练小雪"
        ));
        
        // 调教相关关键词
        TAG_KEYWORDS.put("discipline", Arrays.asList(
            "调教小雪"
        ));
    }
    
    /**
     * 分析章节内容并生成标签
     * @param chapterId 章节ID
     * @return 生成的标签列表
     */
    @Transactional
    public List<ChapterTag> analyzeAndGenerateTags(Long chapterId) {
        // 检查是否已有标签
        List<ChapterTag> existingTags = chapterTagRepository.findByChapterId(chapterId);
        if (!existingTags.isEmpty()) {
            return existingTags; // 已有标签，直接返回
        }
        
        // 获取章节内容
        Optional<Chapter> chapterOpt = chapterRepository.findById(chapterId);
        if (!chapterOpt.isPresent()) {
            return new ArrayList<>();
        }
        
        Chapter chapter = chapterOpt.get();
        String content = chapter.getContent();
        if (content == null || content.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        // 分析内容生成标签
        List<ChapterTag> newTags = new ArrayList<>();
        
        for (Map.Entry<String, List<String>> entry : TAG_KEYWORDS.entrySet()) {
            String tagType = entry.getKey();
            List<String> keywords = entry.getValue();
            
            Set<String> detectedKeywords = new HashSet<>();
            for (String keyword : keywords) {
                if (content.contains(keyword)) {
                    detectedKeywords.add(keyword);
                }
            }
            
            // 如果检测到关键词，创建标签
            if (!detectedKeywords.isEmpty()) {
                String tagValue = getTagDisplayName(tagType);
                String keywordsStr = String.join(",", detectedKeywords);
                
                ChapterTag tag = new ChapterTag(chapterId, tagType, tagValue, keywordsStr);
                newTags.add(tag);
            }
        }
        
        // 保存新标签
        if (!newTags.isEmpty()) {
            chapterTagRepository.saveAll(newTags);
        }
        
        return newTags;
    }
    
    /**
     * 批量分析小说的所有章节
     * @param novelId 小说ID
     * @return 章节ID到标签列表的映射
     */
    @Transactional
    public Map<Long, List<ChapterTag>> batchAnalyzeNovelChapters(Long novelId) {
        // 先查询该小说的所有现有标签
        List<ChapterTag> existingTags = chapterTagRepository.findByNovelId(novelId);
        Map<Long, List<ChapterTag>> chapterTagsMap = new HashMap<>();
        
        // 整理现有标签
        for (ChapterTag tag : existingTags) {
            chapterTagsMap.computeIfAbsent(tag.getChapterId(), k -> new ArrayList<>()).add(tag);
        }
        
        // 获取小说的所有章节
        List<Chapter> chapters = chapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId);
        
        // 分析尚未标记的章节
        for (Chapter chapter : chapters) {
            Long chapterId = chapter.getId();
            if (!chapterTagsMap.containsKey(chapterId)) {
                List<ChapterTag> newTags = analyzeAndGenerateTags(chapterId);
                if (!newTags.isEmpty()) {
                    chapterTagsMap.put(chapterId, newTags);
                }
            }
        }
        
        return chapterTagsMap;
    }
    
    /**
     * 获取章节的所有标签
     * @param chapterId 章节ID
     * @return 标签列表
     */
    public List<ChapterTag> getChapterTags(Long chapterId) {
        return chapterTagRepository.findByChapterId(chapterId);
    }
    
    /**
     * 删除章节的所有标签
     * @param chapterId 章节ID
     */
    @Transactional
    public void deleteChapterTags(Long chapterId) {
        chapterTagRepository.deleteByChapterId(chapterId);
    }
    
    /**
     * 获取标签显示名称
     */
    private String getTagDisplayName(String tagType) {
        switch (tagType) {
            case "punishment":
                return "惩罚";
            case "reward":
                return "奖励";
            case "training":
                return "训练";
            case "discipline":
                return "调教";
            default:
                return tagType;
        }
    }
    
    /**
     * 获取所有支持的标签类型
     */
    public Set<String> getAllTagTypes() {
        return TAG_KEYWORDS.keySet();
    }
}
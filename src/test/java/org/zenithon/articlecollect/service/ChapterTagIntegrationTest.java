package org.zenithon.articlecollect.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.zenithon.articlecollect.entity.Chapter;
import org.zenithon.articlecollect.entity.Novel;
import org.zenithon.articlecollect.repository.ChapterRepository;
import org.zenithon.articlecollect.repository.NovelRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class ChapterTagIntegrationTest {

    @Autowired
    private NovelService novelService;
    
    @Autowired
    private ChapterTagService chapterTagService;
    
    @Autowired
    private NovelRepository novelRepository;
    
    @Autowired
    private ChapterRepository chapterRepository;

    @Test
    public void testChapterTagGeneration() {
        // 创建测试小说
        Novel novel = new Novel("测试小说");
        novel = novelRepository.save(novel);
        
        // 创建包含关键词的测试章节
        String contentWithKeywords = "今天老师严厉地惩罚了不听话的学生，作为奖励，表现好的同学得到了表扬。学校安排了专门的训练课程来调教学生们的行为规范。";
        
        Chapter chapter = new Chapter(novel.getId(), "第一章：入学第一天", contentWithKeywords, 1);
        chapter = chapterRepository.save(chapter);
        
        // 测试标签分析
        List<org.zenithon.articlecollect.entity.ChapterTag> tags = chapterTagService.analyzeAndGenerateTags(chapter.getId());
        
        // 验证标签生成
        assertFalse(tags.isEmpty(), "应该生成标签");
        
        // 验证标签类型
        boolean hasPunishment = tags.stream().anyMatch(tag -> "punishment".equals(tag.getTagType()));
        boolean hasReward = tags.stream().anyMatch(tag -> "reward".equals(tag.getTagType()));
        boolean hasTraining = tags.stream().anyMatch(tag -> "training".equals(tag.getTagType()));
        boolean hasDiscipline = tags.stream().anyMatch(tag -> "discipline".equals(tag.getTagType()));
        
        assertTrue(hasPunishment, "应该检测到惩罚标签");
        assertTrue(hasReward, "应该检测到奖励标签");
        assertTrue(hasTraining, "应该检测到训练标签");
        assertTrue(hasDiscipline, "应该检测到调教标签");
        
        // 验证缓存功能 - 第二次调用应该直接从数据库读取
        List<org.zenithon.articlecollect.entity.ChapterTag> cachedTags = chapterTagService.analyzeAndGenerateTags(chapter.getId());
        assertEquals(tags.size(), cachedTags.size(), "缓存的标签数量应该相同");
        
        // 清理测试数据
        chapterRepository.delete(chapter);
        novelRepository.delete(novel);
    }
}
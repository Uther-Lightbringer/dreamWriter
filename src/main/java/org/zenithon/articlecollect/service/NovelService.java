package org.zenithon.articlecollect.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zenithon.articlecollect.entity.Novel;
import org.zenithon.articlecollect.entity.Chapter;
import org.zenithon.articlecollect.entity.ChapterDetailView;
import org.zenithon.articlecollect.repository.NovelRepository;
import org.zenithon.articlecollect.repository.ChapterRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 小说管理服务类
 */
@Service
public class NovelService {
    
    @Autowired
    private NovelRepository novelRepository;
    
    @Autowired
    private ChapterRepository chapterRepository;
    
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
            return novel;
        }
        return null;
    }
    
    /**
     * 获取所有小说，按创建时间倒序排列（最新的在最前面）
     */
    public List<Novel> getAllNovels() {
        List<Novel> novels = novelRepository.findAllByOrderByCreateTimeDesc();
        // 为所有小说确保格式化时间字段被正确设置
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
            
            // 删除小说
            novelRepository.deleteById(novelId);
            return true;
        }
        return false;
    }
    
    /**
     * 创建小说章节
     */
    public Chapter createChapter(Long novelId, String title, String content) {
        // 检查小说是否存在
        if (!novelRepository.existsById(novelId)) {
            throw new RuntimeException("小说不存在，ID: " + novelId);
        }
        
        // 计算章节序号
        int chapterNumber = chapterRepository.findByNovelIdOrderByChapterNumberAsc(novelId).size() + 1;
        
        Chapter chapter = new Chapter(novelId, title, content, chapterNumber);
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
}
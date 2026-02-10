package org.zenithon.articlecollect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.zenithon.articlecollect.entity.ChapterTag;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChapterTagRepository extends JpaRepository<ChapterTag, Long> {
    
    /**
     * 根据章节ID查找所有标签
     */
    List<ChapterTag> findByChapterId(Long chapterId);
    
    /**
     * 根据章节ID和标签类型查找标签
     */
    Optional<ChapterTag> findByChapterIdAndTagType(Long chapterId, String tagType);
    
    /**
     * 检查章节是否已有特定类型的标签
     */
    boolean existsByChapterIdAndTagType(Long chapterId, String tagType);
    
    /**
     * 根据小说ID查找所有章节标签（用于批量查询优化）
     */
    @Query("SELECT ct FROM ChapterTag ct WHERE ct.chapterId IN (SELECT c.id FROM Chapter c WHERE c.novelId = :novelId)")
    List<ChapterTag> findByNovelId(@Param("novelId") Long novelId);
    
    /**
     * 批量删除指定章节的所有标签
     */
    void deleteByChapterId(Long chapterId);
}
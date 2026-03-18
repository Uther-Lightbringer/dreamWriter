package org.zenithon.articlecollect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zenithon.articlecollect.entity.AiImageHistory;

import java.util.List;

/**
 * AI 图片历史记录 Repository
 */
@Repository
public interface AiImageHistoryRepository extends JpaRepository<AiImageHistory, Long> {
    
    /**
     * 按创建时间倒序查询所有记录
     */
    List<AiImageHistory> findAllByOrderByCreateTimeDesc();
}

package org.zenithon.articlecollect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zenithon.articlecollect.entity.Novel;

import java.util.List;

@Repository
public interface NovelRepository extends JpaRepository<Novel, Long> {
    /**
     * 按创建时间倒序获取所有小说
     */
    List<Novel> findAllByOrderByCreateTimeDesc();
}
package org.zenithon.articlecollect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zenithon.articlecollect.entity.CreativeMemory;

import java.util.List;
import java.util.Optional;

/**
 * 跨会话记忆 Repository
 */
@Repository
public interface CreativeMemoryRepository extends JpaRepository<CreativeMemory, Long> {

    /**
     * 根据 key 查找记忆
     */
    Optional<CreativeMemory> findByKey(String key);

    /**
     * 查找所有记忆，按更新时间倒序
     */
    List<CreativeMemory> findAllByOrderByUpdateTimeDesc();

    /**
     * 检查 key 是否存在
     */
    boolean existsByKey(String key);

    /**
     * 根据 key 删除记忆
     */
    void deleteByKey(String key);
}

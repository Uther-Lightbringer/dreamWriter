package org.zenithon.articlecollect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zenithon.articlecollect.entity.CreativeMemory;

import java.util.List;
import java.util.Optional;

/**
 * 跨会话记忆 Repository
 *
 * 支持两种记忆模式：
 * - 全局记忆（sessionId = null）：跨会话共享
 * - 会话记忆（sessionId != null）：特定会话独有
 */
@Repository
public interface CreativeMemoryRepository extends JpaRepository<CreativeMemory, Long> {

    /**
     * 根据 key 查找记忆（全局）
     */
    Optional<CreativeMemory> findByKey(String key);

    /**
     * 根据 key 和 sessionId 查找记忆
     */
    Optional<CreativeMemory> findByKeyAndSessionId(String key, String sessionId);

    /**
     * 查找所有记忆，按更新时间倒序
     */
    List<CreativeMemory> findAllByOrderByUpdateTimeDesc();

    /**
     * 查找全局记忆（sessionId = null）
     */
    List<CreativeMemory> findBySessionIdIsNullOrderByUpdateTimeDesc();

    /**
     * 查找特定会话的记忆
     */
    List<CreativeMemory> findBySessionIdOrderByUpdateTimeDesc(String sessionId);

    /**
     * 查找特定会话的记忆和全局记忆（合并查询）
     */
    List<CreativeMemory> findBySessionIdOrSessionIdIsNullOrderByUpdateTimeDesc(String sessionId);

    /**
     * 检查 key 是否存在（全局）
     */
    boolean existsByKey(String key);

    /**
     * 检查 key 在特定会话中是否存在
     */
    boolean existsByKeyAndSessionId(String key, String sessionId);

    /**
     * 根据 key 删除记忆（全局）
     */
    void deleteByKey(String key);

    /**
     * 根据 key 和 sessionId 删除记忆
     */
    void deleteByKeyAndSessionId(String key, String sessionId);

    /**
     * 删除特定会话的所有记忆
     */
    void deleteBySessionId(String sessionId);
}

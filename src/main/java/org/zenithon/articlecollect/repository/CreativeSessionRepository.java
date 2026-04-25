package org.zenithon.articlecollect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zenithon.articlecollect.entity.CreativeSession;

import java.util.List;
import java.util.Optional;

/**
 * 创作引导会话 Repository
 */
@Repository
public interface CreativeSessionRepository extends JpaRepository<CreativeSession, Long> {

    /**
     * 根据 sessionId 查找会话
     */
    Optional<CreativeSession> findBySessionId(String sessionId);

    /**
     * 根据状态查找会话列表
     */
    List<CreativeSession> findByStatusOrderByUpdateTimeDesc(CreativeSession.SessionStatus status);

    /**
     * 查找所有会话，按更新时间倒序
     */
    List<CreativeSession> findAllByOrderByUpdateTimeDesc();

    /**
     * 根据 sessionId 删除会话
     */
    void deleteBySessionId(String sessionId);

    /**
     * 检查 sessionId 是否存在
     */
    boolean existsBySessionId(String sessionId);
}

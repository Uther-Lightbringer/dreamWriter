package org.zenithon.articlecollect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zenithon.articlecollect.entity.GenreMemory;
import java.util.List;

/**
 * 体裁记忆Repository
 */
@Repository
public interface GenreMemoryRepository extends JpaRepository<GenreMemory, Long> {

    List<GenreMemory> findBySessionIdAndScope(String sessionId, String scope);

    List<GenreMemory> findBySessionId(String sessionId);

    void deleteBySessionId(String sessionId);
}

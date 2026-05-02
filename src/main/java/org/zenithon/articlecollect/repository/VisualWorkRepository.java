package org.zenithon.articlecollect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zenithon.articlecollect.entity.VisualWork;
import java.util.List;

@Repository
public interface VisualWorkRepository extends JpaRepository<VisualWork, Long> {
    List<VisualWork> findBySessionIdOrderByCreateTimeDesc(String sessionId);
    List<VisualWork> findAllByOrderByCreateTimeDesc();
}

package org.zenithon.articlecollect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zenithon.articlecollect.entity.Essay;
import java.util.List;

@Repository
public interface EssayRepository extends JpaRepository<Essay, Long> {
    List<Essay> findBySessionIdOrderByCreateTimeDesc(String sessionId);
    List<Essay> findAllByOrderByCreateTimeDesc();
}

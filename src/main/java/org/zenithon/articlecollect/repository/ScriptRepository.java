package org.zenithon.articlecollect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zenithon.articlecollect.entity.Script;
import java.util.List;

@Repository
public interface ScriptRepository extends JpaRepository<Script, Long> {
    List<Script> findBySessionIdOrderByCreateTimeDesc(String sessionId);
    List<Script> findAllByOrderByCreateTimeDesc();
}

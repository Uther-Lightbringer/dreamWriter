package org.zenithon.articlecollect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zenithon.articlecollect.entity.ScriptScene;
import java.util.List;

@Repository
public interface ScriptSceneRepository extends JpaRepository<ScriptScene, Long> {
    List<ScriptScene> findByScriptIdOrderBySceneNumberAsc(Long scriptId);
    void deleteByScriptId(Long scriptId);
}

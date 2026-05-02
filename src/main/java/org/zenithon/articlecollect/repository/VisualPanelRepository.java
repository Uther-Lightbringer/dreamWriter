package org.zenithon.articlecollect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zenithon.articlecollect.entity.VisualPanel;
import java.util.List;

@Repository
public interface VisualPanelRepository extends JpaRepository<VisualPanel, Long> {
    List<VisualPanel> findByWorkIdOrderByPanelNumberAsc(Long workId);
    void deleteByWorkId(Long workId);
}

package org.zenithon.articlecollect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zenithon.articlecollect.entity.VisualImage;

import java.util.List;

public interface VisualImageRepository extends JpaRepository<VisualImage, Long> {
    List<VisualImage> findByGroupIdOrderByPanelNumberAsc(Long groupId);
    void deleteByGroupId(Long groupId);
}

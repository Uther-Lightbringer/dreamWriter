package org.zenithon.articlecollect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zenithon.articlecollect.entity.VisualImageGroup;

import java.util.List;

public interface VisualImageGroupRepository extends JpaRepository<VisualImageGroup, Long> {
    List<VisualImageGroup> findByWorkIdOrderByCreateTimeDesc(Long workId);
}

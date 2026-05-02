package org.zenithon.articlecollect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zenithon.articlecollect.entity.EssayParagraph;
import java.util.List;

@Repository
public interface EssayParagraphRepository extends JpaRepository<EssayParagraph, Long> {
    List<EssayParagraph> findByEssayIdOrderByParagraphNumberAsc(Long essayId);
    void deleteByEssayId(Long essayId);
}

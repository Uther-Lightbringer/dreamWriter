package org.zenithon.articlecollect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zenithon.articlecollect.entity.Novel;

@Repository
public interface NovelRepository extends JpaRepository<Novel, Long> {
}
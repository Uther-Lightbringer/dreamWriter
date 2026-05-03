package org.zenithon.articlecollect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zenithon.articlecollect.entity.GenreSession;
import java.util.List;
import java.util.Optional;

/**
 * 体裁会话Repository
 */
@Repository
public interface GenreSessionRepository extends JpaRepository<GenreSession, Long> {

    Optional<GenreSession> findBySessionId(String sessionId);

    List<GenreSession> findByGenreTypeOrderByUpdateTimeDesc(String genreType);

    List<GenreSession> findByGenreTypeAndHasUserMessageTrueOrderByUpdateTimeDesc(String genreType);

    List<GenreSession> findAllByOrderByUpdateTimeDesc();

    void deleteBySessionId(String sessionId);
}

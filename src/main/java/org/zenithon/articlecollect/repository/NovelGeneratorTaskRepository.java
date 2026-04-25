package org.zenithon.articlecollect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zenithon.articlecollect.entity.NovelGeneratorTask;

import java.util.List;
import java.util.Optional;

/**
 * 小说生成器任务Repository
 */
@Repository
public interface NovelGeneratorTaskRepository extends JpaRepository<NovelGeneratorTask, Long> {

    /**
     * 根据任务ID查找任务
     */
    Optional<NovelGeneratorTask> findByTaskId(String taskId);

    /**
     * 根据状态查找任务
     */
    List<NovelGeneratorTask> findByStatusOrderByCreateTimeDesc(NovelGeneratorTask.TaskStatus status);

    /**
     * 查找所有任务，按创建时间倒序
     */
    List<NovelGeneratorTask> findAllByOrderByCreateTimeDesc();

    /**
     * 查找所有任务，按更新时间倒序
     */
    List<NovelGeneratorTask> findAllByOrderByUpdateTimeDesc();

    /**
     * 根据生成的小说ID查找任务
     */
    Optional<NovelGeneratorTask> findByResultNovelId(Long novelId);

    /**
     * 检查任务ID是否存在
     */
    boolean existsByTaskId(String taskId);
}

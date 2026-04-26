package org.zenithon.articlecollect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zenithon.articlecollect.entity.CharacterCardEntity;

import java.util.List;
import java.util.Optional;

/**
 * 角色卡数据访问接口
 */
@Repository
public interface CharacterCardRepository extends JpaRepository<CharacterCardEntity, Long> {
    
    /**
     * 根据小说 ID 查询所有角色卡（按排序顺序）
     */
    List<CharacterCardEntity> findByNovelIdOrderBySortOrderAsc(Long novelId);
    
    /**
     * 删除指定小说的所有角色卡
     */
    void deleteByNovelId(Long novelId);

    /**
     * 按小说ID和角色类型查询角色卡
     */
    List<CharacterCardEntity> findByNovelIdAndRole(Long novelId, String role);

    /**
     * 检查是否存在指定小说ID和角色类型的角色卡
     */
    boolean existsByNovelIdAndRole(Long novelId, String role);
}

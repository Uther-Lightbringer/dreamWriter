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
     * 根据小说 ID 和角色 ID 查询角色卡
     */
    Optional<CharacterCardEntity> findByNovelIdAndCharacterId(Long novelId, String characterId);
    
    /**
     * 根据小说 ID 和角色 ID 查询角色卡（返回 null 如果不存在）
     */
    default CharacterCardEntity findByNovelIdAndCharacterIdOrNull(Long novelId, String characterId) {
        return findByNovelIdAndCharacterId(novelId, characterId).orElse(null);
    }
    
    /**
     * 删除指定小说的所有角色卡
     */
    void deleteByNovelId(Long novelId);
    
    /**
     * 检查角色是否存在
     */
    boolean existsByNovelIdAndCharacterId(Long novelId, String characterId);
}

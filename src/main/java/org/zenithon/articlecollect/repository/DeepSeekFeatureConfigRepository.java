package org.zenithon.articlecollect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.zenithon.articlecollect.entity.DeepSeekFeatureConfig;
import org.zenithon.articlecollect.entity.DeepSeekFeatureConfig.FeatureCode;

import java.util.List;
import java.util.Optional;

/**
 * DeepSeek 功能配置 Repository
 */
@Repository
public interface DeepSeekFeatureConfigRepository extends JpaRepository<DeepSeekFeatureConfig, Long> {

    /**
     * 根据功能代码查询配置
     */
    Optional<DeepSeekFeatureConfig> findByFeatureCode(FeatureCode featureCode);

    /**
     * 查询所有配置，按 ID 排序
     */
    @Query("SELECT c FROM DeepSeekFeatureConfig c ORDER BY c.id")
    List<DeepSeekFeatureConfig> findAllOrderById();

    /**
     * 检查功能代码是否存在
     */
    boolean existsByFeatureCode(FeatureCode featureCode);
}

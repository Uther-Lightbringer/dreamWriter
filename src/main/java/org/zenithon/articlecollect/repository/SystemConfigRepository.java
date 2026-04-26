package org.zenithon.articlecollect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.zenithon.articlecollect.entity.SystemConfig;

import java.util.Optional;

/**
 * 系统配置 Repository
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {

    Optional<SystemConfig> findByConfigKey(String configKey);

    void deleteByConfigKey(String configKey);
}

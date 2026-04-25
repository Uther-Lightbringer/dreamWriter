package org.zenithon.articlecollect.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zenithon.articlecollect.dto.DeepSeekConfigDTO;
import org.zenithon.articlecollect.entity.DeepSeekFeatureConfig;
import org.zenithon.articlecollect.entity.DeepSeekFeatureConfig.FeatureCode;
import org.zenithon.articlecollect.service.DeepSeekConfigService;

import java.util.List;

/**
 * DeepSeek 配置 API 控制器
 */
@RestController
@RequestMapping("/api/deepseek-config")
public class DeepSeekConfigController {

    private final DeepSeekConfigService service;

    public DeepSeekConfigController(DeepSeekConfigService service) {
        this.service = service;
    }

    /**
     * 获取所有功能配置
     */
    @GetMapping
    public ResponseEntity<List<DeepSeekFeatureConfig>> getAllConfigs() {
        return ResponseEntity.ok(service.getAllConfigs());
    }

    /**
     * 获取单个功能配置
     */
    @GetMapping("/{featureCode}")
    public ResponseEntity<DeepSeekFeatureConfig> getConfig(
            @PathVariable String featureCode) {
        try {
            FeatureCode code = FeatureCode.valueOf(featureCode.toUpperCase());
            return ResponseEntity.ok(service.getConfig(code));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 更新功能配置
     */
    @PutMapping("/{featureCode}")
    public ResponseEntity<DeepSeekFeatureConfig> updateConfig(
            @PathVariable String featureCode,
            @RequestBody DeepSeekConfigDTO dto) {
        try {
            FeatureCode code = FeatureCode.valueOf(featureCode.toUpperCase());
            DeepSeekFeatureConfig updated = service.updateConfig(code, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 重置功能配置为默认值
     */
    @PostMapping("/{featureCode}/reset")
    public ResponseEntity<DeepSeekFeatureConfig> resetConfig(
            @PathVariable String featureCode) {
        try {
            FeatureCode code = FeatureCode.valueOf(featureCode.toUpperCase());

            // 获取枚举的默认值
            DeepSeekConfigDTO defaultDto = new DeepSeekConfigDTO();

            if (code == FeatureCode.CREATIVE_GUIDANCE) {
                defaultDto.setModel("deepseek-v4-pro");
                defaultDto.setThinkingEnabled(true);
                defaultDto.setReasoningEffort("high");
            } else {
                defaultDto.setModel("deepseek-v4-flash");
                defaultDto.setThinkingEnabled(false);
                defaultDto.setReasoningEffort("high");
            }

            DeepSeekFeatureConfig reset = service.updateConfig(code, defaultDto);
            return ResponseEntity.ok(reset);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

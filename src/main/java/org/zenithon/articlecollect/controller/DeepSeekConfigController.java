package org.zenithon.articlecollect.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.zenithon.articlecollect.config.DeepSeekConfig;
import org.zenithon.articlecollect.dto.DeepSeekConfigDTO;
import org.zenithon.articlecollect.entity.DeepSeekFeatureConfig;
import org.zenithon.articlecollect.entity.DeepSeekFeatureConfig.FeatureCode;
import org.zenithon.articlecollect.service.DeepSeekConfigService;

import java.util.*;

/**
 * DeepSeek 配置 API 控制器
 */
@RestController
@RequestMapping("/api/deepseek-config")
public class DeepSeekConfigController {

    private static final Logger logger = LoggerFactory.getLogger(DeepSeekConfigController.class);

    private final DeepSeekConfigService service;
    private final DeepSeekConfig deepSeekConfig;
    private final RestTemplate restTemplate;

    public DeepSeekConfigController(DeepSeekConfigService service, DeepSeekConfig deepSeekConfig, RestTemplate restTemplate) {
        this.service = service;
        this.deepSeekConfig = deepSeekConfig;
        this.restTemplate = restTemplate;
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

    /**
     * 查询 DeepSeek 账户余额
     */
    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getBalance() {
        String apiKey = deepSeekConfig.getApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "API Key 未配置");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            // DeepSeek 余额查询 API
            String balanceUrl = "https://api.deepseek.com/user/balance";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    balanceUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                result.put("isAvailable", body.get("is_available"));

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> balanceInfos = (List<Map<String, Object>>) body.get("balance_infos");
                if (balanceInfos != null && !balanceInfos.isEmpty()) {
                    Map<String, Object> balanceInfo = balanceInfos.get(0);
                    result.put("currency", balanceInfo.get("currency"));
                    result.put("totalBalance", balanceInfo.get("total_balance"));
                    result.put("grantedBalance", balanceInfo.get("granted_balance"));
                    result.put("toppedUpBalance", balanceInfo.get("topped_up_balance"));
                }
            }

            logger.info("查询 DeepSeek 余额成功");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("查询 DeepSeek 余额失败: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "查询失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 计算费用估算
     * DeepSeek 定价（单位：元/百万 tokens）:
     * - 输入（缓存命中）: 0.5
     * - 输入（缓存未命中）: 2.0
     * - 输出: 8.0
     */
    @PostMapping("/estimate-cost")
    public ResponseEntity<Map<String, Object>> estimateCost(@RequestBody Map<String, Object> usage) {
        try {
            long promptTokens = ((Number) usage.getOrDefault("prompt_tokens", 0)).longValue();
            long completionTokens = ((Number) usage.getOrDefault("completion_tokens", 0)).longValue();
            long cacheHitTokens = ((Number) usage.getOrDefault("prompt_cache_hit_tokens", 0)).longValue();
            long cacheMissTokens = ((Number) usage.getOrDefault("prompt_cache_miss_tokens", 0)).longValue();

            // 如果没有缓存信息，用 prompt_tokens 作为缓存未命中
            if (cacheHitTokens == 0 && cacheMissTokens == 0) {
                cacheMissTokens = promptTokens;
            }

            // DeepSeek 定价（元/百万 tokens）
            final double PRICE_CACHE_HIT = 0.5;
            final double PRICE_CACHE_MISS = 2.0;
            final double PRICE_OUTPUT = 8.0;

            // 计算费用
            double cacheHitCost = (cacheHitTokens / 1_000_000.0) * PRICE_CACHE_HIT;
            double cacheMissCost = (cacheMissTokens / 1_000_000.0) * PRICE_CACHE_MISS;
            double outputCost = (completionTokens / 1_000_000.0) * PRICE_OUTPUT;
            double totalCost = cacheHitCost + cacheMissCost + outputCost;

            // 计算节省的费用（如果没有缓存，全部按未命中计算）
            double costWithoutCache = (promptTokens / 1_000_000.0) * PRICE_CACHE_MISS + outputCost;
            double savedCost = costWithoutCache - totalCost;

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("totalCost", String.format("%.6f", totalCost));
            result.put("totalCostYuan", totalCost);

            // 详细费用
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("cacheHitCost", String.format("%.6f", cacheHitCost));
            details.put("cacheMissCost", String.format("%.6f", cacheMissCost));
            details.put("outputCost", String.format("%.6f", outputCost));
            details.put("savedCost", String.format("%.6f", savedCost));
            details.put("cacheHitTokens", cacheHitTokens);
            details.put("cacheMissTokens", cacheMissTokens);
            details.put("completionTokens", completionTokens);
            result.put("details", details);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("计算费用失败: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}

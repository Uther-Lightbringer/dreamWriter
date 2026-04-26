package org.zenithon.articlecollect.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.zenithon.articlecollect.config.EvoLinkConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EvoLinkImageService {

    private static final Logger logger = LoggerFactory.getLogger(EvoLinkImageService.class);

    private final EvoLinkConfig evoLinkConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SystemConfigService systemConfigService;

    public EvoLinkImageService(EvoLinkConfig evoLinkConfig, RestTemplate restTemplate,
                               ObjectMapper objectMapper, SystemConfigService systemConfigService) {
        this.evoLinkConfig = evoLinkConfig;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.systemConfigService = systemConfigService;
    }

    /**
     * 获取当前配置的图片生成模型
     */
    private String getCurrentModel() {
        return systemConfigService.getConfigValue("image.model", "z-image-turbo");
    }

    /**
     * 创建图片生成任务（无seed）
     */
    public String generateImage(String prompt, String size) {
        return generateImage(prompt, size, null);
    }

    /**
     * 创建图片生成任务（支持seed参数）
     * @param prompt 提示词
     * @param size 图片尺寸
     * @param seed 随机种子（可选，1-2147483647），相同seed+相同prompt产生相似结果
     * @return 任务ID
     */
    public String generateImage(String prompt, String size, Integer seed) {
        if (evoLinkConfig.getApiToken() == null || evoLinkConfig.getApiToken().trim().isEmpty()) {
            throw new RuntimeException("EvoLink API Token 未配置");
        }
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + evoLinkConfig.getApiToken());

            // 获取当前配置的模型
            String model = getCurrentModel();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("prompt", prompt);
            requestBody.put("size", size);

            logger.info("使用模型: {}", model);

            // z-image-turbo 专用参数：seed
            if ("z-image-turbo".equals(model)) {
                if (seed != null && seed >= 1 && seed <= 2147483647) {
                    requestBody.put("seed", seed);
                    logger.info("使用seed参数: {}", seed);
                }
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                evoLinkConfig.getApiUrl(),
                request,
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                String taskId = rootNode.path("id").asText();
                
                if (taskId != null && !taskId.isEmpty()) {
                    logger.info("图片生成任务已创建：{}", taskId);
                    return taskId;
                } else {
                    throw new RuntimeException("未能获取任务 ID");
                }
            } else {
                throw new RuntimeException("创建任务失败：" + response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("创建图片生成任务失败：" + e.getMessage(), e);
            throw new RuntimeException("创建图片生成任务失败：" + e.getMessage(), e);
        }
    }

    /**
     * 创建图片生成任务（支持所有参数）
     * 根据模型类型自动添加对应参数：
     * - z-image-turbo: 支持 seed 参数
     * - gpt-image-2: 支持 resolution, quality, n, image_urls 参数
     *
     * @param prompt 提示词
     * @param size 图片尺寸
     * @param seed 随机种子（可选，z-image-turbo专用）
     * @param resolution 分辨率（可选，gpt-image-2专用，如 "1024x1024"）
     * @param quality 图片质量（可选，gpt-image-2专用，如 "high", "medium", "low"）
     * @param n 生成图片数量（可选，gpt-image-2专用，1-10）
     * @param imageUrls 参考图片URL列表（可选，gpt-image-2专用）
     * @return 任务ID
     */
    public String generateImage(String prompt, String size, Integer seed,
                                String resolution, String quality, Integer n,
                                List<String> imageUrls) {
        if (evoLinkConfig.getApiToken() == null || evoLinkConfig.getApiToken().trim().isEmpty()) {
            throw new RuntimeException("EvoLink API Token 未配置");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + evoLinkConfig.getApiToken());

            // 获取当前配置的模型
            String model = getCurrentModel();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("prompt", prompt);
            requestBody.put("size", size);

            logger.info("使用模型: {}", model);

            // z-image-turbo 专用参数：seed
            if ("z-image-turbo".equals(model)) {
                if (seed != null && seed >= 1 && seed <= 2147483647) {
                    requestBody.put("seed", seed);
                    logger.info("使用 seed 参数: {}", seed);
                }
            }

            // gpt-image-2 专用参数
            if ("gpt-image-2".equals(model)) {
                // resolution 参数
                if (resolution != null && !resolution.isEmpty()) {
                    requestBody.put("resolution", resolution);
                    logger.info("使用 resolution 参数: {}", resolution);
                }

                // quality 参数
                if (quality != null && !quality.isEmpty()) {
                    requestBody.put("quality", quality);
                    logger.info("使用 quality 参数: {}", quality);
                }

                // n 参数（生成图片数量）
                if (n != null && n >= 1 && n <= 10) {
                    requestBody.put("n", n);
                    logger.info("使用 n 参数: {}", n);
                }

                // image_urls 参数（参考图片）
                if (imageUrls != null && !imageUrls.isEmpty()) {
                    requestBody.put("image_urls", imageUrls);
                    logger.info("使用 image_urls 参数，数量: {}", imageUrls.size());
                }
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                evoLinkConfig.getApiUrl(),
                request,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                String taskId = rootNode.path("id").asText();

                if (taskId != null && !taskId.isEmpty()) {
                    logger.info("图片生成任务已创建：{}", taskId);
                    return taskId;
                } else {
                    throw new RuntimeException("未能获取任务 ID");
                }
            } else {
                throw new RuntimeException("创建任务失败：" + response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("创建图片生成任务失败：" + e.getMessage(), e);
            throw new RuntimeException("创建图片生成任务失败：" + e.getMessage(), e);
        }
    }

    /**
     * 根据角色名称生成固定的seed值
     * 用于角色一致性：同一角色每次生成使用相同的seed
     * @param characterName 角色名称
     * @return seed值 (1-2147483647)
     */
    public static Integer generateSeedForCharacter(String characterName) {
        if (characterName == null || characterName.isEmpty()) {
            return null;
        }
        // 使用hashCode生成seed，确保为正数且在有效范围内
        int hash = Math.abs(characterName.hashCode());
        // 确保在1到2147483647范围内
        return (hash % 2147483646) + 1;
    }

    public TaskStatus getTaskStatus(String taskId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + evoLinkConfig.getApiToken());
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            String url = "https://api.evolink.ai/v1/tasks/" + taskId;
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseTaskStatus(response.getBody());
            } else {
                throw new RuntimeException("查询任务状态失败：" + response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("查询任务状态失败：" + e.getMessage(), e);
            throw new RuntimeException("查询任务状态失败：" + e.getMessage(), e);
        }
    }
    
    private TaskStatus parseTaskStatus(String responseBody) throws Exception {
        JsonNode rootNode = objectMapper.readTree(responseBody);
        
        TaskStatus status = new TaskStatus();
        status.setId(rootNode.path("id").asText());
        status.setStatus(rootNode.path("status").asText());
        status.setProgress(rootNode.path("progress").asInt(0));
        
        JsonNode resultsNode = rootNode.path("results");
        if (resultsNode.isArray() && resultsNode.size() > 0) {
            status.setImageUrl(resultsNode.get(0).asText());
        }
        
        JsonNode errorNode = rootNode.path("error");
        if (!errorNode.isMissingNode()) {
            status.setError(errorNode.path("message").asText(null));
        }
        
        return status;
    }
    
    public static class TaskStatus {
        private String id;
        private String status;
        private int progress;
        private String imageUrl;
        private String error;
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public boolean isCompleted() { return "completed".equals(status); }
        public boolean isFailed() { return "failed".equals(status); }
    }
}

package org.zenithon.articlecollect.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * EvoLink 图片生成工具类
 * 可在任何地方调用，支持同步和异步模式
 */
@Component
public class EvoLinkImageUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(EvoLinkImageUtil.class);
    
    private static RestTemplate restTemplate;
    private static ObjectMapper objectMapper;
    private static String apiToken;
    private static String apiUrl;
    private static String model;
    
    /**
     * 初始化工具类 (需要在应用启动时调用)
     */
    public static void init(RestTemplate restTemplate, ObjectMapper objectMapper, 
                           String apiToken, String apiUrl, String model) {
        EvoLinkImageUtil.restTemplate = restTemplate;
        EvoLinkImageUtil.objectMapper = objectMapper;
        EvoLinkImageUtil.apiToken = apiToken;
        EvoLinkImageUtil.apiUrl = apiUrl;
        EvoLinkImageUtil.model = model;
    }
    
    /**
     * 生成图片 (返回任务 ID)
     * @param prompt 提示词
     * @param size 图片尺寸 (如 "16:9" 或 "1024x768")
     * @return 任务 ID
     */
    public static String generateImageAsync(String prompt, String size) {
        checkInitialized();
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiToken);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("prompt", prompt);
            requestBody.put("size", size);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);
            
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
     * 查询任务状态
     * @param taskId 任务 ID
     * @return 任务状态信息
     */
    public static TaskStatus getTaskStatus(String taskId) {
        checkInitialized();
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiToken);
            
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
    
    /**
     * 轮询等待任务完成 (阻塞式)
     * @param taskId 任务 ID
     * @param pollIntervalMs 轮询间隔 (毫秒)
     * @param timeoutMs 超时时间 (毫秒)
     * @return 最终的任务状态
     */
    public static TaskStatus waitForCompletion(String taskId, long pollIntervalMs, long timeoutMs) {
        long startTime = System.currentTimeMillis();
        
        while (true) {
            TaskStatus status = getTaskStatus(taskId);
            
            if ("completed".equals(status.getStatus())) {
                logger.info("图片生成完成：{}", taskId);
                return status;
            }
            
            if ("failed".equals(status.getStatus())) {
                logger.error("图片生成失败：{} - {}", taskId, status.getError());
                return status;
            }
            
            // 检查超时
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                logger.warn("等待图片生成超时：{}", taskId);
                return status;
            }
            
            // 等待一段时间后再次查询
            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("等待过程被中断", e);
                return null;
            }
        }
    }
    
    /**
     * 验证尺寸格式
     */
    public static boolean validateSize(String size) {
        if (size == null || size.trim().isEmpty()) {
            return false;
        }
        
        // 检查比例格式
        if (size.contains(":")) {
            String[] validRatios = {"1:1", "2:3", "3:2", "3:4", "4:3", "9:16", "16:9", "1:2", "2:1"};
            for (String ratio : validRatios) {
                if (ratio.equals(size)) {
                    return true;
                }
            }
            return false;
        }
        
        // 检查自定义尺寸
        if (size.contains("x")) {
            try {
                String[] parts = size.split("x");
                if (parts.length != 2) {
                    return false;
                }
                
                int width = Integer.parseInt(parts[0]);
                int height = Integer.parseInt(parts[1]);
                
                return width >= 376 && width <= 1536 && height >= 376 && height <= 1536;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        return false;
    }
    
    private static void checkInitialized() {
        if (restTemplate == null || objectMapper == null || apiToken == null) {
            throw new IllegalStateException("EvoLinkImageUtil 未初始化，请先调用 init() 方法");
        }
    }
    
    private static TaskStatus parseTaskStatus(String responseBody) throws Exception {
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
    
    /**
     * 任务状态类
     */
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

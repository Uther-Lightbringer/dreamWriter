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
import java.util.Map;

@Service
public class EvoLinkImageService {
    
    private static final Logger logger = LoggerFactory.getLogger(EvoLinkImageService.class);
    
    private final EvoLinkConfig evoLinkConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public EvoLinkImageService(EvoLinkConfig evoLinkConfig, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.evoLinkConfig = evoLinkConfig;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    public String generateImage(String prompt, String size) {
        if (evoLinkConfig.getApiToken() == null || evoLinkConfig.getApiToken().trim().isEmpty()) {
            throw new RuntimeException("EvoLink API Token 未配置");
        }
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + evoLinkConfig.getApiToken());
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", evoLinkConfig.getModel());
            requestBody.put("prompt", prompt);
            requestBody.put("size", size);
            requestBody.put("seed", 1);
            
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

package org.zenithon.articlecollect.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.zenithon.articlecollect.config.DeepSeekConfig;
import org.zenithon.articlecollect.dto.CharacterCard;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * DeepSeek AI 绘画提示词生成服务
 */
@Service
public class AIPromptService {
    
    private static final Logger logger = LoggerFactory.getLogger(AIPromptService.class);
    
    private final DeepSeekConfig deepSeekConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public AIPromptService(DeepSeekConfig deepSeekConfig, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.deepSeekConfig = deepSeekConfig;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 根据角色卡信息生成 AI 绘画提示词
     * @param characterCard 角色卡信息
     * @return 生成的 AI 绘画提示词，如果失败则返回 null
     */
    public String generateAIPrompt(CharacterCard characterCard) {
        // 检查 API Key 是否配置
        if (deepSeekConfig.getApiKey() == null || deepSeekConfig.getApiKey().trim().isEmpty()) {
            logger.warn("DeepSeek API Key 未配置，跳过 AI 提示词生成");
            return null;
        }
        
        try {
            // 构建角色描述
            String characterDescription = buildCharacterDescription(characterCard);
            
            // 构建请求 prompt
            String prompt = buildPrompt(characterDescription);
            
            // 调用 DeepSeek API
            String aiResponse = callDeepSeekAPI(prompt);
            
            // 解析响应并提取提示词，限制长度在1800字符以下
            if (aiResponse != null && !aiResponse.trim().isEmpty()) {
                String trimmedPrompt = trimPromptToLength(aiResponse, 1800);
                logger.info("AI 绘画提示词生成成功，长度: {} 字符", trimmedPrompt.length());
                return trimmedPrompt;
            }
        } catch (Exception e) {
            logger.error("生成 AI 绘画提示词失败：" + e.getMessage(), e);
        }
        
        return null;
    }
    
    /**
     * 构建角色描述文本
     */
    private String buildCharacterDescription(CharacterCard card) {
        StringBuilder sb = new StringBuilder();
        
        // 基本信息
        sb.append("角色姓名：").append(card.getName()).append("\n");
        
        if (card.getAge() != null) {
            sb.append("年龄：").append(card.getAge()).append("\n");
        }
        
        if (card.getGender() != null && !card.getGender().isEmpty()) {
            sb.append("性别：").append(card.getGender()).append("\n");
        }
        
        if (card.getOccupation() != null && !card.getOccupation().isEmpty()) {
            sb.append("职业：").append(card.getOccupation()).append("\n");
        }

        // 外貌特征
        if (card.getAppearance() != null) {
            sb.append("\n【外貌特征】\n");
            if (card.getAppearance().getHeight() != null && !card.getAppearance().getHeight().isEmpty()) {
                sb.append("- 身高：").append(card.getAppearance().getHeight()).append("\n");
            }
            if (card.getAppearance().getHair() != null && !card.getAppearance().getHair().isEmpty()) {
                sb.append("- 发色：").append(card.getAppearance().getHair()).append("\n");
            }
            if (card.getAppearance().getEyes() != null && !card.getAppearance().getEyes().isEmpty()) {
                sb.append("- 瞳色：").append(card.getAppearance().getEyes()).append("\n");
            }
            if (card.getAppearance().getBuild() != null && !card.getAppearance().getBuild().isEmpty()) {
                sb.append("- 体型：").append(card.getAppearance().getBuild()).append("\n");
            }
            if (card.getAppearance().getDistinguishingFeatures() != null && !card.getAppearance().getDistinguishingFeatures().isEmpty()) {
                sb.append("- 显著特征：").append(card.getAppearance().getDistinguishingFeatures()).append("\n");
            }
        }

        // 性格描述
        if (card.getPersonality() != null && !card.getPersonality().isEmpty()) {
            sb.append("\n【性格描述】\n").append(card.getPersonality()).append("\n");
        }
        
        // 背景故事
        if (card.getBackground() != null && !card.getBackground().isEmpty()) {
            sb.append("\n【背景故事】\n").append(card.getBackground()).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 构建 AI 提示词生成的 Prompt - 精简版，只描述画面内容
     */
    private String buildPrompt(String characterDescription) {
        return "你是AI绘画提示词生成器。根据角色信息，生成简洁的画面描述。\n\n" +

               "【核心原则】\n" +
               "- 只描述画面中能看到的东西\n" +
               "- 不要解释性文字、不要情感描述、不要背景故事\n" +
               "- 不要形容词堆砌，只保留必要的视觉描述\n" +
               "- 不要写【画面呈现】【氛围】等抽象词汇\n\n" +

               "【必须包含的画面元素】\n" +
               "1. 人物：性别、年龄感、体型、发型发色、瞳色、肤色\n" +
               "2. 服装：颜色、款式、材质\n" +
               "3. 姿势：站立/坐着/躺着等具体姿态\n" +
               "4. 表情：具体的面部表情\n" +
               "5. 场景：背景中实际出现的环境和物体\n" +
               "6. 光线：光源方向和强度\n\n" +

               "【输出格式】\n" +
               "- 用逗号分隔的关键词形式\n" +
               "- 一行输出，无换行\n" +
               "- 100-300字以内\n" +
               "- 只用中文\n" +
               "- 不输出任何开场白或解释\n\n" +

               "【示例】\n" +
               "输入：张三，25岁男性，黑发短发光头，棕色眼睛，身穿白色衬衫黑色西裤，站着微笑，办公室背景\n" +
               "输出：25岁男性，短发黑发，棕色眼睛，白色衬衫，黑色西裤，站立姿势，微笑表情，办公室背景，日光灯照明，写实摄影风格\n\n" +

               "角色信息：\n" +
               characterDescription;
    }
    
    /**
     * 调用 DeepSeek API (同步模式)
     * @param prompt 提示词
     * @return AI 返回的内容
     * @throws Exception 调用异常
     */
    public String callDeepSeekAPI(String prompt) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + deepSeekConfig.getApiKey());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", deepSeekConfig.getModel());
        requestBody.put("max_tokens", 1200);

        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        requestBody.put("messages", new Object[]{message});

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(
            deepSeekConfig.getApiUrl(), 
            request, 
            String.class
        );
        
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            // 解析 JSON 响应
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode choices = rootNode.path("choices");
            
            if (choices.isArray() && choices.size() > 0) {
                JsonNode messageNode = choices.get(0).path("message");
                String content = messageNode.path("content").asText();
                
                if (content != null && !content.isEmpty()) {
                    return content.trim();
                }
            }
        }
        
        return null;
    }
    
    /**
     * 流式 AI 对话
     * @param prompt 用户输入的提示词
     * @param emitter SSE 发射器
     * @throws Exception 可能抛出的异常
     */
    public void chatStream(String prompt, SseEmitter emitter) throws Exception {
        // 检查 API Key 是否配置
        if (deepSeekConfig.getApiKey() == null || deepSeekConfig.getApiKey().trim().isEmpty()) {
            logger.warn("DeepSeek API Key 未配置");
            emitter.send(SseEmitter.event()
                .name("error")
                .data("{\"error\": \"DeepSeek API Key 未配置，请在 application.properties 中配置 deepseek.api.key\"}"));
            return;
        }
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + deepSeekConfig.getApiKey());
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", deepSeekConfig.getModel());
            requestBody.put("stream", true); // 启用流式响应
            
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            requestBody.put("messages", new Object[]{message});
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // 使用 RestTemplate 执行流式请求
            ResponseEntity<String> response = restTemplate.postForEntity(
                deepSeekConfig.getApiUrl(), 
                request, 
                String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // 处理流式响应
                String responseBody = response.getBody();
                String[] lines = responseBody.split("\\n");
                
                StringBuilder fullContent = new StringBuilder();
                
                for (String line : lines) {
                    if (line.trim().isEmpty() || line.startsWith(":")) {
                        continue;
                    }
                    
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);
                        
                        if ("[DONE]".equals(data.trim())) {
                            break;
                        }
                        
                        try {
                            JsonNode jsonNode = objectMapper.readTree(data);
                            JsonNode choices = jsonNode.path("choices");
                            
                            if (choices.isArray() && choices.size() > 0) {
                                JsonNode delta = choices.get(0).path("delta");
                                String content = delta.path("content").asText();
                                
                                if (content != null && !content.isEmpty()) {
                                    fullContent.append(content);
                                    
                                    // 发送数据块到前端（保留原始空格）
                                    emitter.send(SseEmitter.event()
                                        .name("message")
                                        .data(content));
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("解析 SSE 数据块失败：" + e.getMessage());
                        }
                    }
                }
                
                logger.info("流式对话完成，总内容长度：{}", fullContent.length());
            } else {
                throw new RuntimeException("API 调用失败：" + response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("流式对话失败：" + e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 将提示词截断到指定字符数以内
     * 在最后一个空格或标点处截断，避免截断单词
     */
    private String trimPromptToLength(String prompt, int maxLength) {
        if (prompt == null || prompt.length() <= maxLength) {
            return prompt;
        }

        // 先尝试在最后一个空格处截断
        int lastSpace = prompt.lastIndexOf(' ', maxLength);
        if (lastSpace > maxLength * 0.7) { // 确保不会截断太短
            return prompt.substring(0, lastSpace);
        }

        // 尝试在标点符号处截断
        char[] punctuation = {'.', ',', '!', '?', ';', ':'};
        for (char p : punctuation) {
            int lastPunc = prompt.lastIndexOf(p, maxLength);
            if (lastPunc > maxLength * 0.7) {
                return prompt.substring(0, lastPunc + 1);
            }
        }

        // 如果没有找到合适的截断点，就直接截断
        logger.warn("提示词过长({}字符)，已截断到{}字符", prompt.length(), maxLength);
        return prompt.substring(0, maxLength);
    }
}

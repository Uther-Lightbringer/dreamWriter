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
     * 构建 AI 提示词生成的 Prompt - 使用高级提示词结构（中文）
     */
    private String buildPrompt(String characterDescription) {
        return "你是一位专业的AI绘画提示词工程师。请根据以下角色信息生成高质量、详细的AI图像生成中文提示词。\n\n" +

               "【高级提示词结构要求】\n" +
               "请按以下结构生成提示词：\n" +
               "1. **基本结构**：[主体] + [动作/状态] + [背景/环境]\n" +
               "2. **风格修饰词**：例如 \"cyberpunk style\"（赛博朋克风格）、\"anime style\"（动漫风格）、\"watercolor style\"（水彩风格）等\n" +
               "3. **具体细节**：包含构图、视角、色彩、光线、纹理等信息\n" +
               "4. **镜头角度**：例如 \"wide-angle lens shot\"（广角镜头）、\"medium shot\"（中景）、\"close-up shot\"（特写）等\n" +
               "5. **情感基调**：描述场景的情绪或氛围，例如 \"mysterious\"（神秘）、\"warm and cozy\"（温暖舒适）、\"epic\"（史诗感）等\n" +
               "6. **艺术家参考**：例如 \"in the style of Hayao Miyazaki\"（宫崎骏风格）、\"in the style of Van Gogh\"（梵高风格）等\n" +
               "7. **光线描述**：例如 \"soft morning light\"（柔和晨光）、\"cinematic lighting\"（电影光线）、\"dramatic lighting\"（戏剧性光线）等\n" +
               "8. **纹理/材质**：例如 \"smooth marble texture\"（光滑大理石纹理）、\"fabric texture\"（织物纹理）等\n" +
               "9. **构图**：例如 \"rule of thirds composition\"（三分法构图）、\"symmetrical composition\"（对称构图）等\n\n" +

               "【角色肖像提示词具体要求】\n" +
               "- **艺术风格**：photorealistic（写实）、real person photography（真人摄影）、ultra HD（超高清）、professional photo quality（专业照片质量）\n" +
               "- **构图**：根据角色选择（全身照/半身照/特写/中景等）\n" +
               "- **姿势**：设计自然且富有表现力的姿势（站立/坐姿/动态姿势/战斗姿态等）\n" +
               "- **视角**：选择最佳视角（平视/低角度/高角度/侧面/正面等）\n" +
               "- **关键描述**：外观特征（发型、发色、眼神、表情、面部特征）、身材体型、服装细节、动作姿势\n" +
               "- **场景/背景**：添加与角色气质相符的场景和氛围\n" +
               "- **光线**：光效的详细描述，自然光、柔和影棚光\n" +
               "- **色彩**：描述主色调和色彩方案，自然肤色、写实色彩\n" +
               "- **图像质量**：ultra HD, 8k分辨率, high detail（高细节）, sharp focus（锐利对焦）, professional photography（专业摄影）\n\n" +

               "【输出格式要求】\n" +
               "- **必须风格**：始终包含 photorealistic, real person photography, ultra HD, 8k, professional DSLR photo quality\n" +
               "- 只输出提示词文本，不要其他解释\n" +
               "- 长度在200-500词之间，**绝对不要超过1800字符**\n" +
               "- **最多1800字符 - 这很关键！**\n" +
               "- **只用英文输出**\n" +
               "- **保持简洁明了** - 保持描述清晰直接，避免过于复杂的句子\n" +
               "- 专注于最重要的视觉元素，避免不必要的装饰细节\n" +
               "- **输出一行，不要分段** - 所有文本必须在一行连续，没有换行\n" +
               "- **使用关键词，不要用完整句子** - 列出风格、镜头角度、光线等项目时，使用逗号分隔的关键词而不是完整句子\n" +
               "- 避免不当内容，必要时使用同义词\n\n" +

               "【提示词优化示例】\n" +
               "原始想法：\"一只老鹰\"\n" +
               "优化后的提示词：\"A fierce eagle, in vibrant Japanese anime style, blending Studio Ghibli's detailed backgrounds with dynamic shonen manga action scenes. The eagle has exaggerated, expressive eyes with a determined gaze, feathers rendered with sharp, dynamic lines suggesting motion. Its wings are spread wide, massive wingspan filling the frame. The eagle wears a small samurai-inspired piece of armor on its chest, adding a touch of fantasy. Background blends traditional Japanese elements like cherry blossoms and Mount Fuji with a futuristic Tokyo skyline contrast. Scene features bright, saturated colors, dramatic lighting effects and speed lines emphasizing the eagle's power and agility. Overall composition creates energy and movement, typical of action manga scene style.\"\n\n" +

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

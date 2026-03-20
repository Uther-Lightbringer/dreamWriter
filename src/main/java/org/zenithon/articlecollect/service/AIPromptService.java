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
     * 构建 AI 提示词生成的 Prompt - 使用高级提示词结构（英文输出）
     */
    private String buildPrompt(String characterDescription) {
        return "You are a professional AI image prompt engineer. Please generate a high-quality, detailed prompt for AI image generation based on the following character information.\n\n" +

               "【Advanced Prompt Structure Requirements】\n" +
               "Please generate the prompt following this structure:\n" +
               "1. **Basic Structure**: [Subject] + [Action/State] + [Background/Environment]\n" +
               "2. **Style Modifiers**: e.g., \"cyberpunk style\", \"Studio Ghibli style\", \"Makoto Shinkai style\", \"Michelangelo style\", etc.\n" +
               "3. **Specific Details**: Include information about composition, perspective, colors, lighting, textures, etc.\n" +
               "4. **Camera Angles**: e.g., \"wide-angle lens shot\", \"bird's eye view\", \"medium shot\", \"close-up shot\", etc.\n" +
               "5. **Emotional Tone**: Describe the mood or atmosphere of the scene, e.g., \"mysterious\", \"warm and cozy\", \"epic\", etc.\n" +
               "6. **Artist References**: e.g., \"in the style of Hayao Miyazaki\", \"in the style of Makoto Shinkai\", \"in the style of Van Gogh\", etc.\n" +
               "7. **Lighting Description**: e.g., \"soft morning light\", \"cyberpunk neon lights\", \"cinematic lighting\", \"dramatic lighting\", etc.\n" +
               "8. **Texture/Material**: e.g., \"smooth marble texture\", \"polished metal surface\", \"fabric texture\", etc.\n" +
               "9. **Composition**: e.g., \"rule of thirds composition\", \"symmetrical composition\", \"center composition\", etc.\n\n" +

               "【Character Portrait Prompt Specific Requirements】\n" +
               "- **Art Style**: photorealistic, realistic portrait, real person photography, authentic photo style\n" +
               "- **Composition**: Choose based on character (full body shot/half body shot/close-up/medium shot, etc.)\n" +
               "- **Pose**: Design natural and expressive poses (standing/sitting/dynamic pose/fighting stance, etc.)\n" +
               "- **Perspective**: Choose the best perspective (eye level/low angle/high angle/profile/front view, etc.)\n" +
               "- **Key Descriptions**: Appearance features (hair style, hair color, eye color, expression, facial features), body type, clothing details, action pose\n" +
               "- **Scene/Background**: Add scene and atmosphere matching the character's temperament\n" +
               "- **Lighting**: Detailed description of lighting effects, natural lighting, soft studio lighting\n" +
               "- **Colors**: Describe main color palette and color scheme, natural skin tones, realistic colors\n" +
               "- **Image Quality**: ultra HD, 8k resolution, high detail, sharp focus, professional photography, DSLR quality, photorealistic\n\n" +

               "【Output Format Requirements】\n" +
               "- **MANDATORY STYLE**: Always include photorealistic, real person photography, ultra HD, 8k, professional DSLR photo quality\n" +
               "- Only output the prompt text, no other explanations\n" +
               "- Length between 200-500 words, **NEVER EXCEED 1800 CHARACTERS**\n" +
               "- **MAX 1800 CHARACTERS - THIS IS CRITICAL!**\n" +
               "- **USE ENGLISH ONLY**\n" +
               "- **KEEP IT SIMPLE AND CONCISE** - Keep descriptions clear and direct, avoid overly complex sentences\n" +
               "- Focus on the most important visual elements, avoid unnecessary decorative details\n" +
               "- **OUTPUT AS A SINGLE LINE, NO PARAGRAPHS** - All text must be on one continuous line without line breaks\n" +
               "- **USE KEYWORDS, NOT FULL SENTENCES** - When listing items like styles, camera angles, lighting, etc., use comma-separated keywords instead of complete sentences\n" +
               "- Avoid inappropriate content, use synonyms when necessary\n\n" +

               "【Prompt Optimization Example】\n" +
               "Original idea: \"an eagle\"\n" +
               "Optimized prompt: \"A fierce eagle, in vibrant Japanese anime style, blending Studio Ghibli's detailed backgrounds with dynamic shonen manga action scenes. The eagle has exaggerated, expressive eyes with a determined gaze, feathers rendered with sharp, dynamic lines suggesting motion. Its wings are spread wide, massive wingspan filling the frame. The eagle wears a small samurai-inspired piece of armor on its chest, adding a touch of fantasy. Background blends traditional Japanese elements like cherry blossoms and Mount Fuji with a futuristic Tokyo skyline contrast. Scene features bright, saturated colors, dramatic lighting effects and speed lines emphasizing the eagle's power and agility. Overall composition creates energy and movement, typical of action manga scene style.\"\n\n" +

               "Character Information:\n" +
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
        requestBody.put("max_tokens", 1700);

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

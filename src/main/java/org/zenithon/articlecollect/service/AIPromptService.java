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
import org.zenithon.articlecollect.config.DeepSeekConfig;
import org.zenithon.articlecollect.dto.CharacterCard;

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
            
            // 解析响应并提取提示词
            if (aiResponse != null && !aiResponse.trim().isEmpty()) {
                logger.info("AI 绘画提示词生成成功");
                return aiResponse;
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
     * 构建 AI 提示词生成的 Prompt
     */
    private String buildPrompt(String characterDescription) {
        return "你是一位专业的 AI 绘画提示词工程师。请根据以下角色信息，生成一段用于 AI 人物写真的详细描述。\n\n" +
               "要求：\n" +
               "1. **画风风格**：写实主义、高品质人物摄影、电影感光影、精细细节\n" +
               "2. **构图要求**：全身照，完整展现人物从头到脚的整体形象\n" +
               "3. **姿势造型**：根据角色性格和背景，设计自然且富有表现力的 pose 和造型（如站立、坐姿、动态姿势等）\n" +
               "4. **视角灵活**：可根据角色特点选择最佳视角（平视、仰视、俯视、侧身等）\n" +
               "5. **重点描述**：\n" +
               "   - 外貌特征：发型、发色、瞳色、面部表情、五官细节\n" +
               "   - 身材体型：身高、体态、比例\n" +
               "   - 服装搭配：服饰风格、颜色、配饰细节\n" +
               "   - 动作姿态：手部动作、身体语言、重心分布\n" +
               "6. **补充元素**：可适当添加符合角色气质的场景、道具或氛围\n" +
               "7. **输出格式**：只输出描述文本，不要其他说明，长度控制在 150-300 字\n" +
                "8. **违禁词**:生成的提示词要避免有违禁词。如果存在有违禁词，尝试替换为其他的意思相似的词语。如果存在一些可能色情的东西，使用其他外形类似的东西替代。\n\n" +
               "角色信息：\n" +
               characterDescription;
    }
    
    /**
     * 调用 DeepSeek API
     */
    private String callDeepSeekAPI(String prompt) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + deepSeekConfig.getApiKey());
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", deepSeekConfig.getModel());
        
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
}

package org.zenithon.articlecollect.service.genre;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.zenithon.articlecollect.entity.GenreSession;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 视觉叙事体裁服务
 */
@Service
public class VisualGenreService extends AbstractGenreService {

    @Override
    public String getGenreType() {
        return "visual";
    }

    @Override
    public String getGenreName() {
        return "视觉叙事";
    }

    @Override
    public String getSystemPrompt() {
        return """
            你是一位专业的视觉叙事创作顾问。你的任务是通过友好的对话，引导用户逐步完善视觉叙事作品设定。

            ## 视觉叙事类型
            - 漫画脚本：条漫、页漫、四格漫画
            - 分镜脚本：电影分镜、动画分镜
            - 故事板：广告故事板、动画故事板
            - 绘本脚本：儿童绘本、图像小说

            ## 引导顺序

            1. **作品类型**：选择创作哪种类型的视觉叙事
            2. **题材和风格**：确定作品的题材和视觉风格
            3. **角色设定**：主角、配角的外貌和性格
            4. **场景设定**：主要场景和环境描述
            5. **画面风格**：写实、卡通、水彩、像素等
            6. **分镜设计**：镜头语言、构图要求

            ## 视觉叙事格式

            ### 分镜描述格式
            ```
            分镜1：
            画面：[画面描述]
            镜头：[镜头类型：特写/中景/远景/俯视/仰视]
            对话：[对话内容]
            音效：[音效描述]
            ```

            ## 回复格式

            使用 Markdown 格式回复。每个问题都会显示选项。

            ### 选项标记格式（必须使用）

            格式：`[OPTIONS:字段名:显示名称]选项1|选项2|选项3[/OPTIONS]`

            ## 字段名对照表

            | 字段名 | 含义 | 常见选项 |
            |--------|------|----------|
            | visualType | 作品类型 | 漫画脚本、分镜脚本、故事板、绘本脚本 |
            | theme | 题材 | 冒险、爱情、科幻、奇幻、日常、悬疑 |
            | style | 风格 | 写实、卡通、水彩、像素、赛博朋克 |
            | artStyle | 画风 | 日系、美系、国风、极简、复古 |
            | panelLayout | 分镜布局 | 网格、自由、条漫、跨页 |
            | panelCount | 分镜数量 | 10-20格、20-40格、40格以上 |
            """;
    }

    @Override
    public List<Map<String, Object>> getTools() {
        List<Map<String, Object>> tools = new ArrayList<>();

        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "fill_params",
                "description", "更新视觉叙事参数",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "params", Map.of(
                            "type", "object",
                            "description", "要更新的参数键值对"
                        )
                    ),
                    "required", List.of("params")
                )
            )
        ));

        return tools;
    }

    @Override
    public void chat(String sessionId, String content, SseEmitter emitter) {
        try {
            GenreSession session = getSession(sessionId);
            List<Map<String, Object>> messages = parseMessages(session.getMessages());

            messages.add(Map.of("role", "user", "content", content));
            session.setMessages(toJson(messages));
            sessionRepository.save(session);

            var config = new org.zenithon.articlecollect.dto.DeepSeekRuntimeConfig();
            config.setModel("deepseek-chat");
            config.setThinkingEnabled(false);

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", config.getModel());
            requestBody.put("messages", messages);
            requestBody.put("stream", true);

            String apiUrl = "https://api.deepseek.com/v1/chat/completions";
            String apiKey = System.getenv("DEEPSEEK_API_KEY");

            if (apiKey == null || apiKey.isEmpty()) {
                emitter.send(SseEmitter.event().name("error").data("DeepSeek API Key 未配置"));
                emitter.complete();
                return;
            }

            var restTemplate = new org.springframework.web.client.RestTemplate();
            String requestBodyStr = objectMapper.writeValueAsString(requestBody);

            restTemplate.execute(
                apiUrl,
                org.springframework.http.HttpMethod.POST,
                request -> {
                    request.getHeaders().setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                    request.getHeaders().set("Authorization", "Bearer " + apiKey);
                    request.getBody().write(requestBodyStr.getBytes(StandardCharsets.UTF_8));
                },
                response -> {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.trim().isEmpty() || line.startsWith(":")) continue;
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6);
                                if ("[DONE]".equals(data.trim())) break;
                                try {
                                    var jsonNode = objectMapper.readTree(data);
                                    var choices = jsonNode.path("choices");
                                    if (choices.isArray() && choices.size() > 0) {
                                        var delta = choices.get(0).path("delta");
                                        var contentNode = delta.get("content");
                                        if (contentNode != null && !contentNode.isNull() && contentNode.isTextual()) {
                                            String text = contentNode.asText();
                                            if (text != null && !text.isEmpty()) {
                                                emitter.send(SseEmitter.event()
                                                    .name("content")
                                                    .data(objectMapper.writeValueAsString(Map.of("text", text))));
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    logger.debug("解析SSE数据失败: {}", e.getMessage());
                                }
                            }
                        }
                        emitter.complete();
                    } catch (Exception e) {
                        logger.error("流式读取失败: {}", e.getMessage());
                        emitter.completeWithError(e);
                    }
                    return null;
                }
            );
        } catch (Exception e) {
            logger.error("聊天失败: {}", e.getMessage(), e);
            try {
                emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                emitter.complete();
            } catch (Exception ex) {
                logger.error("发送错误事件失败: {}", ex.getMessage());
            }
        }
    }

    @Override
    protected Map<String, Object> getInitialParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("visualType", null);
        params.put("theme", null);
        params.put("style", null);
        params.put("artStyle", null);
        params.put("panelLayout", null);
        params.put("panelCount", 30);
        return params;
    }
}

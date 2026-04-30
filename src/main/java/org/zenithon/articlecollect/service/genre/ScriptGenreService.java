package org.zenithon.articlecollect.service.genre;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.zenithon.articlecollect.entity.GenreSession;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 剧本体裁服务
 */
@Service
public class ScriptGenreService extends AbstractGenreService {

    @Override
    public String getGenreType() {
        return "script";
    }

    @Override
    public String getGenreName() {
        return "剧本";
    }

    @Override
    public String getSystemPrompt() {
        return """
            你是一位专业的剧本创作顾问。你的任务是通过友好的对话，引导用户逐步完善剧本设定。

            ## 剧本类型
            - 影视剧本：电影、电视剧、网剧
            - 舞台剧本：话剧、音乐剧
            - 广播剧脚本：有声剧、广播剧

            ## 引导顺序

            1. **剧本类型**：选择创作哪种类型的剧本
            2. **题材和风格**：确定剧本的题材和整体风格
            3. **角色设定**：主角、配角的基本信息和性格
            4. **场景设定**：主要场景和环境描述
            5. **剧情结构**：三幕式、五幕式或其他结构
            6. **对白风格**：文艺、口语化、方言等

            ## 剧本格式

            ### 场景描述格式
            ```
            场景：室内 - 客厅 - 白天
            [场景描述：宽敞明亮的客厅，阳光透过落地窗洒在地板上]
            ```

            ### 角色对白格式
            ```
            角色名：（动作描述）对白内容
            ```

            ## 回复格式

            使用 Markdown 格式回复。每个问题都会显示选项。

            ### 选项标记格式（必须使用）

            格式：`[OPTIONS:字段名:显示名称]选项1|选项2|选项3[/OPTIONS]`

            ## 字段名对照表

            | 字段名 | 含义 | 常见选项 |
            |--------|------|----------|
            | scriptType | 剧本类型 | 影视剧本、舞台剧本、广播剧脚本 |
            | theme | 题材 | 爱情、悬疑、喜剧、历史、科幻、家庭 |
            | style | 风格 | 写实、荒诞、浪漫、黑色幽默、严肃 |
            | structure | 剧情结构 | 三幕式、五幕式、线性、非线性 |
            | dialogueStyle | 对白风格 | 文艺、口语化、方言、诗意 |
            | actCount | 幕数 | 3幕、5幕、多幕 |
            """;
    }

    @Override
    public List<Map<String, Object>> getTools() {
        List<Map<String, Object>> tools = new ArrayList<>();

        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "fill_params",
                "description", "更新剧本参数",
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
        params.put("scriptType", null);
        params.put("theme", null);
        params.put("style", null);
        params.put("structure", null);
        params.put("dialogueStyle", null);
        params.put("actCount", 3);
        return params;
    }
}

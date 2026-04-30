package org.zenithon.articlecollect.service.genre;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.zenithon.articlecollect.entity.GenreSession;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 散文体裁服务
 */
@Service
public class EssayGenreService extends AbstractGenreService {

    @Override
    public String getGenreType() {
        return "essay";
    }

    @Override
    public String getGenreName() {
        return "散文";
    }

    @Override
    public String getSystemPrompt() {
        return """
            你是一位专业的散文创作顾问。你的任务是通过友好的对话，引导用户逐步完善散文设定。

            ## 散文类型
            - 抒情散文：情感表达、意境营造
            - 叙事散文：记人叙事、生活感悟
            - 议论散文：杂文、随笔、评论
            - 写景散文：游记、自然描写
            - 哲理散文：人生感悟、哲理思考

            ## 引导顺序

            1. **散文类型**：选择创作哪种类型的散文
            2. **主题和情感**：确定散文的核心主题和情感基调
            3. **写作视角**：第一人称、第三人称、多视角
            4. **语言风格**：文艺、朴实、诗意、幽默
            5. **结构安排**：线性、散点、意识流

            ## 散文写作特点

            ### 情感表达
            - 真挚自然，避免矫揉造作
            - 通过细节传达情感
            - 情景交融，借景抒情

            ### 语言风格
            - 优美流畅，富有韵律感
            - 善用修辞手法（比喻、拟人、排比等）
            - 长短句交替，创造节奏感

            ## 回复格式

            使用 Markdown 格式回复。每个问题都会显示选项。

            ### 选项标记格式（必须使用）

            格式：`[OPTIONS:字段名:显示名称]选项1|选项2|选项3[/OPTIONS]`

            ## 字段名对照表

            | 字段名 | 含义 | 常见选项 |
            |--------|------|----------|
            | essayType | 散文类型 | 抒情散文、叙事散文、议论散文、写景散文、哲理散文 |
            | theme | 主题 | 亲情、友情、爱情、乡愁、人生、自然、旅行 |
            | emotion | 情感基调 | 温暖、忧伤、怀念、激昂、宁静、感悟 |
            | perspective | 写作视角 | 第一人称、第三人称、多视角 |
            | languageStyle | 语言风格 | 文艺、朴实、诗意、幽默、深沉 |
            | structure | 结构安排 | 线性、散点、意识流、对比 |
            | wordCount | 篇幅 | 800-1500字、1500-3000字、3000字以上 |
            """;
    }

    @Override
    public List<Map<String, Object>> getTools() {
        List<Map<String, Object>> tools = new ArrayList<>();

        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "fill_params",
                "description", "更新散文参数",
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
        params.put("essayType", null);
        params.put("theme", null);
        params.put("emotion", null);
        params.put("perspective", "第一人称");
        params.put("languageStyle", null);
        params.put("structure", null);
        params.put("wordCount", 2000);
        return params;
    }
}

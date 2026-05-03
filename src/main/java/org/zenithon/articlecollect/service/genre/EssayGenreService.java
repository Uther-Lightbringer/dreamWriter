package org.zenithon.articlecollect.service.genre;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.zenithon.articlecollect.entity.GenreSession;
import org.zenithon.articlecollect.entity.Essay;
import org.zenithon.articlecollect.entity.EssayParagraph;
import org.zenithon.articlecollect.repository.EssayRepository;
import org.zenithon.articlecollect.repository.EssayParagraphRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 散文体裁服务
 */
@Service
public class EssayGenreService extends AbstractGenreService {

    @Autowired
    private EssayRepository essayRepository;

    @Autowired
    private EssayParagraphRepository essayParagraphRepository;

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

        // fill_params
        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "fill_params",
                "description", "更新散文参数",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "params", Map.of("type", "object", "description", "要更新的参数键值对")
                    ),
                    "required", List.of("params")
                )
            )
        ));

        // create_essay
        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "create_essay",
                "description", "创建散文",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "title", Map.of("type", "string", "description", "散文标题"),
                        "essayType", Map.of("type", "string", "description", "散文类型：抒情散文、叙事散文、议论散文、写景散文、哲理散文"),
                        "description", Map.of("type", "string", "description", "散文简介")
                    ),
                    "required", List.of("title", "essayType")
                )
            )
        ));

        // add_paragraph
        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "add_paragraph",
                "description", "添加段落",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "essayId", Map.of("type", "integer", "description", "散文ID"),
                        "paragraphNumber", Map.of("type", "integer", "description", "段落序号"),
                        "content", Map.of("type", "string", "description", "段落内容"),
                        "summary", Map.of("type", "string", "description", "段落概括")
                    ),
                    "required", List.of("essayId", "paragraphNumber", "content")
                )
            )
        ));

        // update_paragraph
        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "update_paragraph",
                "description", "修改段落",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "paragraphId", Map.of("type", "integer", "description", "段落ID"),
                        "content", Map.of("type", "string", "description", "段落内容"),
                        "summary", Map.of("type", "string", "description", "段落概括")
                    ),
                    "required", List.of("paragraphId", "content")
                )
            )
        ));

        // get_paragraph_summaries
        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "get_paragraph_summaries",
                "description", "获取散文所有段落概括",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "essayId", Map.of("type", "integer", "description", "散文ID")
                    ),
                    "required", List.of("essayId")
                )
            )
        ));

        // generate_essay_outline
        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "generate_essay_outline",
                "description", "生成散文大纲",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "essayId", Map.of("type", "integer", "description", "散文ID"),
                        "outline", Map.of("type", "string", "description", "大纲内容")
                    ),
                    "required", List.of("essayId", "outline")
                )
            )
        ));

        // update_essay
        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "update_essay",
                "description", "修改散文信息",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "essayId", Map.of("type", "integer", "description", "散文ID"),
                        "title", Map.of("type", "string", "description", "散文标题"),
                        "description", Map.of("type", "string", "description", "散文简介"),
                        "essayType", Map.of("type", "string", "description", "散文类型")
                    ),
                    "required", List.of("essayId")
                )
            )
        ));

        // update_essay_outline
        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "update_essay_outline",
                "description", "更新散文大纲",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "essayId", Map.of("type", "integer", "description", "散文ID"),
                        "outline", Map.of("type", "string", "description", "大纲内容")
                    ),
                    "required", List.of("essayId", "outline")
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
            markSessionHasUserMessage(session);

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("messages", messages);
            requestBody.put("tools", getTools());
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

                        StringBuilder fullContent = new StringBuilder();
                        Map<String, Map<String, Object>> toolCallsMap = new LinkedHashMap<>();
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

                                        // 处理推理内容（reasoning_content）
                                        var reasoningNode = delta.get("reasoning_content");
                                        if (reasoningNode != null && !reasoningNode.isNull() && reasoningNode.isTextual()) {
                                            String reasoningText = reasoningNode.asText();
                                            if (reasoningText != null && !reasoningText.isEmpty()) {
                                                emitter.send(SseEmitter.event()
                                                    .name("reasoning_content")
                                                    .data(objectMapper.writeValueAsString(Map.of("text", reasoningText))));
                                            }
                                        }

                                        var contentNode = delta.get("content");
                                        if (contentNode != null && !contentNode.isNull() && contentNode.isTextual()) {
                                            String text = contentNode.asText();
                                            if (text != null && !text.isEmpty()) {
                                                fullContent.append(text);
                                                emitter.send(SseEmitter.event()
                                                    .name("content")
                                                    .data(objectMapper.writeValueAsString(Map.of("text", text))));
                                            }
                                        }

                                        var toolCallsDelta = delta.path("tool_calls");
                                        if (toolCallsDelta.isArray() && toolCallsDelta.size() > 0) {
                                            for (var tc : toolCallsDelta) {
                                                int index = tc.path("index").asInt(-1);
                                                String toolCallId = tc.path("id").asText(null);
                                                var function = tc.path("function");
                                                String functionName = function.path("name").asText(null);
                                                String argumentsChunk = function.path("arguments").asText(null);

                                                String mapKey = (index >= 0) ? String.valueOf(index) : "_default_";

                                                Map<String, Object> toolCall = toolCallsMap.computeIfAbsent(mapKey, k -> {
                                                    Map<String, Object> tc2 = new LinkedHashMap<>();
                                                    tc2.put("id", "pending_id_" + index);
                                                    tc2.put("type", "function");
                                                    Map<String, Object> func = new LinkedHashMap<>();
                                                    func.put("name", "");
                                                    func.put("arguments", new StringBuilder());
                                                    tc2.put("function", func);
                                                    return tc2;
                                                });

                                                if (toolCallId != null && !toolCallId.isEmpty()) {
                                                    toolCall.put("id", toolCallId);
                                                }

                                                Map<String, Object> func = (Map<String, Object>) toolCall.get("function");
                                                if (functionName != null && !functionName.isEmpty()) {
                                                    func.put("name", functionName);
                                                }
                                                if (argumentsChunk != null && !argumentsChunk.isEmpty()) {
                                                    ((StringBuilder) func.get("arguments")).append(argumentsChunk);
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    logger.debug("解析SSE数据失败: {}", e.getMessage());
                                }
                            }
                        }

                        List<Map<String, Object>> toolCalls = new ArrayList<>();
                        for (Map<String, Object> tc : toolCallsMap.values()) {
                            Map<String, Object> finalTc = new LinkedHashMap<>();
                            String finalId = (String) tc.get("id");
                            if (finalId == null || finalId.startsWith("pending_id_")) {
                                finalId = "auto_id_" + UUID.randomUUID().toString().substring(0, 8);
                            }
                            finalTc.put("id", finalId);
                            finalTc.put("type", tc.get("type"));
                            Map<String, Object> func = (Map<String, Object>) tc.get("function");
                            Map<String, Object> finalFunc = new LinkedHashMap<>();
                            finalFunc.put("name", func.get("name"));
                            finalFunc.put("arguments", func.get("arguments").toString());
                            finalTc.put("function", finalFunc);
                            toolCalls.add(finalTc);
                        }

                        Map<String, Object> assistantMessage = new LinkedHashMap<>();
                        assistantMessage.put("role", "assistant");
                        assistantMessage.put("content", fullContent.toString());
                        if (!toolCalls.isEmpty()) {
                            assistantMessage.put("tool_calls", toolCalls);
                        }
                        messages.add(assistantMessage);

                        if (!toolCalls.isEmpty()) {
                            processToolCalls(toolCalls, emitter, session, messages);
                            session.setMessages(toJson(messages));
                            sessionRepository.save(session);

                            emitter.send(SseEmitter.event().name("tool_calls_done").data("{}"));
                            continueAfterToolCalls(messages, emitter, session, 0);
                        } else {
                            session.setMessages(toJson(messages));
                            sessionRepository.save(session);
                            emitter.complete();
                        }

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

    @SuppressWarnings("unchecked")
    private void processToolCalls(List<Map<String, Object>> toolCalls, SseEmitter emitter,
                                  GenreSession session, List<Map<String, Object>> messages) {
        for (Map<String, Object> toolCall : toolCalls) {
            String toolCallId = (String) toolCall.get("id");
            Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
            String functionName = function != null ? (String) function.get("name") : "";
            String argumentsJson = function != null ? (String) function.get("arguments") : "{}";

            logger.info("处理工具调用: {}({})", functionName, argumentsJson);

            String result;
            boolean success = true;

            try {
                switch (functionName) {
                    case "fill_params":
                        result = executeFillParams(argumentsJson, session);
                        break;
                    case "create_essay":
                        result = executeCreateEssay(argumentsJson, session);
                        break;
                    case "add_paragraph":
                        result = executeAddParagraph(argumentsJson);
                        break;
                    case "update_paragraph":
                        result = executeUpdateParagraph(argumentsJson);
                        break;
                    case "get_paragraph_summaries":
                        result = executeGetParagraphSummaries(argumentsJson);
                        break;
                    case "generate_essay_outline":
                        result = executeGenerateEssayOutline(argumentsJson);
                        break;
                    case "update_essay":
                        result = executeUpdateEssay(argumentsJson);
                        break;
                    case "update_essay_outline":
                        result = executeUpdateEssayOutline(argumentsJson);
                        break;
                    default:
                        result = "{\"error\": \"未知工具: " + functionName + "\"}";
                        success = false;
                }
            } catch (Exception e) {
                logger.error("工具调用 {} 执行失败: {}", functionName, e.getMessage(), e);
                result = "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
                success = false;
            }

            messages.add(Map.of(
                "role", "tool",
                "tool_call_id", toolCallId,
                "content", result
            ));

            try {
                emitter.send(SseEmitter.event()
                    .name("tool_result")
                    .data(objectMapper.writeValueAsString(Map.of(
                        "callId", toolCallId,
                        "tool", functionName,
                        "result", result,
                        "success", success
                    ))));
            } catch (Exception e) {
                logger.warn("发送工具结果事件失败: {}", e.getMessage());
            }
        }
    }

    private void continueAfterToolCalls(List<Map<String, Object>> messages, SseEmitter emitter,
                                         GenreSession session, int depth) {
        if (depth > 3) {
            try { emitter.complete(); } catch (Exception e) { /* ignore */ }
            return;
        }

        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("messages", messages);
            requestBody.put("tools", getTools());
            requestBody.put("stream", true);

            String apiUrl = "https://api.deepseek.com/v1/chat/completions";
            String apiKey = System.getenv("DEEPSEEK_API_KEY");

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

                        StringBuilder fullContent = new StringBuilder();
                        Map<String, Map<String, Object>> toolCallsMap = new LinkedHashMap<>();
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

                                        // 处理推理内容（reasoning_content）
                                        var reasoningNode = delta.get("reasoning_content");
                                        if (reasoningNode != null && !reasoningNode.isNull() && reasoningNode.isTextual()) {
                                            String reasoningText = reasoningNode.asText();
                                            if (reasoningText != null && !reasoningText.isEmpty()) {
                                                emitter.send(SseEmitter.event()
                                                    .name("reasoning_content")
                                                    .data(objectMapper.writeValueAsString(Map.of("text", reasoningText))));
                                            }
                                        }

                                        var contentNode = delta.get("content");
                                        if (contentNode != null && !contentNode.isNull() && contentNode.isTextual()) {
                                            String text = contentNode.asText();
                                            if (text != null && !text.isEmpty()) {
                                                fullContent.append(text);
                                                emitter.send(SseEmitter.event()
                                                    .name("content")
                                                    .data(objectMapper.writeValueAsString(Map.of("text", text))));
                                            }
                                        }

                                        var toolCallsDelta = delta.path("tool_calls");
                                        if (toolCallsDelta.isArray() && toolCallsDelta.size() > 0) {
                                            for (var tc : toolCallsDelta) {
                                                int index = tc.path("index").asInt(-1);
                                                String toolCallId = tc.path("id").asText(null);
                                                var function = tc.path("function");
                                                String functionName = function.path("name").asText(null);
                                                String argumentsChunk = function.path("arguments").asText(null);

                                                String mapKey = (index >= 0) ? String.valueOf(index) : "_default_";

                                                Map<String, Object> toolCall = toolCallsMap.computeIfAbsent(mapKey, k -> {
                                                    Map<String, Object> tc2 = new LinkedHashMap<>();
                                                    tc2.put("id", "pending_id_" + index);
                                                    tc2.put("type", "function");
                                                    Map<String, Object> func = new LinkedHashMap<>();
                                                    func.put("name", "");
                                                    func.put("arguments", new StringBuilder());
                                                    tc2.put("function", func);
                                                    return tc2;
                                                });

                                                if (toolCallId != null && !toolCallId.isEmpty()) {
                                                    toolCall.put("id", toolCallId);
                                                }

                                                Map<String, Object> func = (Map<String, Object>) toolCall.get("function");
                                                if (functionName != null && !functionName.isEmpty()) {
                                                    func.put("name", functionName);
                                                }
                                                if (argumentsChunk != null && !argumentsChunk.isEmpty()) {
                                                    ((StringBuilder) func.get("arguments")).append(argumentsChunk);
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    logger.debug("解析SSE数据失败: {}", e.getMessage());
                                }
                            }
                        }

                        List<Map<String, Object>> toolCalls = new ArrayList<>();
                        for (Map<String, Object> tc : toolCallsMap.values()) {
                            Map<String, Object> finalTc = new LinkedHashMap<>();
                            String finalId = (String) tc.get("id");
                            if (finalId == null || finalId.startsWith("pending_id_")) {
                                finalId = "auto_id_" + UUID.randomUUID().toString().substring(0, 8);
                            }
                            finalTc.put("id", finalId);
                            finalTc.put("type", tc.get("type"));
                            Map<String, Object> func = (Map<String, Object>) tc.get("function");
                            Map<String, Object> finalFunc = new LinkedHashMap<>();
                            finalFunc.put("name", func.get("name"));
                            finalFunc.put("arguments", func.get("arguments").toString());
                            finalTc.put("function", finalFunc);
                            toolCalls.add(finalTc);
                        }

                        Map<String, Object> assistantMessage = new LinkedHashMap<>();
                        assistantMessage.put("role", "assistant");
                        assistantMessage.put("content", fullContent.toString());
                        if (!toolCalls.isEmpty()) {
                            assistantMessage.put("tool_calls", toolCalls);
                        }
                        messages.add(assistantMessage);

                        if (!toolCalls.isEmpty()) {
                            processToolCalls(toolCalls, emitter, session, messages);
                            session.setMessages(toJson(messages));
                            sessionRepository.save(session);

                            emitter.send(SseEmitter.event().name("tool_calls_done").data("{}"));
                            continueAfterToolCalls(messages, emitter, session, depth + 1);
                        } else {
                            session.setMessages(toJson(messages));
                            sessionRepository.save(session);
                            emitter.complete();
                        }

                    } catch (Exception e) {
                        logger.error("流式读取失败: {}", e.getMessage());
                        emitter.completeWithError(e);
                    }
                    return null;
                }
            );
        } catch (Exception e) {
            logger.error("继续生成失败: {}", e.getMessage());
            try { emitter.complete(); } catch (Exception ex) { /* ignore */ }
        }
    }

    // ==================== 工具实现 ====================

    private String executeFillParams(String argumentsJson, GenreSession session) {
        try {
            Map<String, Object> params = fromJson(session.getExtractedParams(), new TypeReference<Map<String, Object>>() {});
            if (params == null) params = new LinkedHashMap<>();

            Map<String, Object> newParams = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
            Map<String, Object> paramsObj = (Map<String, Object>) newParams.get("params");
            if (paramsObj != null) {
                params.putAll(paramsObj);
            }

            session.setExtractedParams(toJson(params));
            sessionRepository.save(session);
            return "{\"success\": true, \"message\": \"参数更新成功\"}";
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String executeCreateEssay(String argumentsJson, GenreSession session) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
            String title = (String) args.get("title");
            String essayType = (String) args.get("essayType");
            String description = (String) args.get("description");

            Essay essay = new Essay();
            essay.setSessionId(session.getSessionId());
            essay.setTitle(title);
            essay.setEssayType(essayType);
            essay.setDescription(description);
            essay = essayRepository.save(essay);

            // 更新会话标题
            updateSessionTitleIfNeeded(session, title);

            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "essayId", essay.getId(),
                "message", "散文《" + title + "》创建成功"
            ));
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String executeAddParagraph(String argumentsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
            Long essayId = Long.valueOf(args.get("essayId").toString());
            Integer paragraphNumber = Integer.valueOf(args.get("paragraphNumber").toString());
            String content = (String) args.get("content");
            String summary = (String) args.get("summary");

            EssayParagraph paragraph = new EssayParagraph();
            paragraph.setEssayId(essayId);
            paragraph.setParagraphNumber(paragraphNumber);
            paragraph.setContent(content);
            paragraph.setSummary(summary);
            paragraph = essayParagraphRepository.save(paragraph);

            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "paragraphId", paragraph.getId(),
                "message", "段落" + paragraphNumber + "添加成功"
            ));
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String executeUpdateParagraph(String argumentsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
            Long paragraphId = Long.valueOf(args.get("paragraphId").toString());
            String content = (String) args.get("content");
            String summary = (String) args.get("summary");

            EssayParagraph paragraph = essayParagraphRepository.findById(paragraphId)
                .orElseThrow(() -> new RuntimeException("段落不存在: " + paragraphId));
            paragraph.setContent(content);
            if (summary != null) paragraph.setSummary(summary);
            essayParagraphRepository.save(paragraph);

            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "message", "段落更新成功"
            ));
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String executeGetParagraphSummaries(String argumentsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
            Long essayId = Long.valueOf(args.get("essayId").toString());

            List<EssayParagraph> paragraphs = essayParagraphRepository.findByEssayIdOrderByParagraphNumberAsc(essayId);
            List<Map<String, Object>> summaries = new ArrayList<>();
            for (EssayParagraph paragraph : paragraphs) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("paragraphId", paragraph.getId());
                item.put("paragraphNumber", paragraph.getParagraphNumber());
                item.put("summary", paragraph.getSummary() != null ? paragraph.getSummary() :
                    paragraph.getContent().substring(0, Math.min(100, paragraph.getContent().length())));
                summaries.add(item);
            }

            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "paragraphs", summaries
            ));
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String executeGenerateEssayOutline(String argumentsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
            Long essayId = Long.valueOf(args.get("essayId").toString());
            String outline = (String) args.get("outline");

            Essay essay = essayRepository.findById(essayId)
                .orElseThrow(() -> new RuntimeException("散文不存在: " + essayId));
            essay.setOutline(outline);
            essayRepository.save(essay);

            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "message", "大纲生成成功"
            ));
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String executeUpdateEssay(String argumentsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
            Long essayId = Long.valueOf(args.get("essayId").toString());

            Essay essay = essayRepository.findById(essayId)
                .orElseThrow(() -> new RuntimeException("散文不存在: " + essayId));

            if (args.containsKey("title")) essay.setTitle((String) args.get("title"));
            if (args.containsKey("description")) essay.setDescription((String) args.get("description"));
            if (args.containsKey("essayType")) essay.setEssayType((String) args.get("essayType"));
            essayRepository.save(essay);

            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "message", "散文信息更新成功"
            ));
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String executeUpdateEssayOutline(String argumentsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
            Long essayId = Long.valueOf(args.get("essayId").toString());
            String outline = (String) args.get("outline");

            Essay essay = essayRepository.findById(essayId)
                .orElseThrow(() -> new RuntimeException("散文不存在: " + essayId));
            essay.setOutline(outline);
            essayRepository.save(essay);

            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "message", "大纲更新成功"
            ));
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
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

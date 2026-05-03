package org.zenithon.articlecollect.service.genre;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.zenithon.articlecollect.entity.GenreSession;
import org.zenithon.articlecollect.entity.Script;
import org.zenithon.articlecollect.entity.ScriptScene;
import org.zenithon.articlecollect.repository.ScriptRepository;
import org.zenithon.articlecollect.repository.ScriptSceneRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 剧本体裁服务
 */
@Service
public class ScriptGenreService extends AbstractGenreService {

    @Autowired
    private ScriptRepository scriptRepository;

    @Autowired
    private ScriptSceneRepository scriptSceneRepository;

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

        // fill_params
        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "fill_params",
                "description", "更新剧本参数",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "params", Map.of("type", "object", "description", "要更新的参数键值对")
                    ),
                    "required", List.of("params")
                )
            )
        ));

        // create_script
        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "create_script",
                "description", "创建新剧本",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "title", Map.of("type", "string", "description", "剧本标题"),
                        "scriptType", Map.of("type", "string", "description", "剧本类型：影视剧本、舞台剧本、广播剧脚本"),
                        "description", Map.of("type", "string", "description", "剧本简介")
                    ),
                    "required", List.of("title", "scriptType")
                )
            )
        ));

        // add_scene
        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "add_scene",
                "description", "添加场景",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "scriptId", Map.of("type", "integer", "description", "剧本ID"),
                        "sceneNumber", Map.of("type", "integer", "description", "场景序号"),
                        "location", Map.of("type", "string", "description", "场景地点"),
                        "time", Map.of("type", "string", "description", "场景时间"),
                        "content", Map.of("type", "string", "description", "场景内容"),
                        "summary", Map.of("type", "string", "description", "场景概括")
                    ),
                    "required", List.of("scriptId", "sceneNumber", "content")
                )
            )
        ));

        // update_scene
        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "update_scene",
                "description", "修改场景",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "sceneId", Map.of("type", "integer", "description", "场景ID"),
                        "content", Map.of("type", "string", "description", "场景内容"),
                        "summary", Map.of("type", "string", "description", "场景概括")
                    ),
                    "required", List.of("sceneId", "content")
                )
            )
        ));

        // get_scene_summaries
        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "get_scene_summaries",
                "description", "获取剧本所有场景概括",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "scriptId", Map.of("type", "integer", "description", "剧本ID")
                    ),
                    "required", List.of("scriptId")
                )
            )
        ));

        // generate_script_outline
        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "generate_script_outline",
                "description", "生成剧本大纲",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "scriptId", Map.of("type", "integer", "description", "剧本ID"),
                        "outline", Map.of("type", "string", "description", "大纲内容")
                    ),
                    "required", List.of("scriptId", "outline")
                )
            )
        ));

        // update_script
        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "update_script",
                "description", "修改剧本信息",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "scriptId", Map.of("type", "integer", "description", "剧本ID"),
                        "title", Map.of("type", "string", "description", "剧本标题"),
                        "description", Map.of("type", "string", "description", "剧本简介"),
                        "scriptType", Map.of("type", "string", "description", "剧本类型")
                    ),
                    "required", List.of("scriptId")
                )
            )
        ));

        // update_script_outline
        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "update_script_outline",
                "description", "更新剧本大纲",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "scriptId", Map.of("type", "integer", "description", "剧本ID"),
                        "outline", Map.of("type", "string", "description", "大纲内容")
                    ),
                    "required", List.of("scriptId", "outline")
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

                                        // 处理内容
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

                                        // 处理工具调用
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
                                                    tc2.put("index", index);
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

                        // 转换工具调用
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

                        // 构建助手消息
                        Map<String, Object> assistantMessage = new LinkedHashMap<>();
                        assistantMessage.put("role", "assistant");
                        assistantMessage.put("content", fullContent.toString());
                        if (!toolCalls.isEmpty()) {
                            assistantMessage.put("tool_calls", toolCalls);
                        }
                        messages.add(assistantMessage);

                        // 处理工具调用
                        if (!toolCalls.isEmpty()) {
                            processToolCalls(toolCalls, emitter, session, messages);
                            session.setMessages(toJson(messages));
                            sessionRepository.save(session);

                            emitter.send(SseEmitter.event().name("tool_calls_done").data("{}"));

                            // 继续调用API
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
                    case "create_script":
                        result = executeCreateScript(argumentsJson, session);
                        break;
                    case "add_scene":
                        result = executeAddScene(argumentsJson);
                        break;
                    case "update_scene":
                        result = executeUpdateScene(argumentsJson);
                        break;
                    case "get_scene_summaries":
                        result = executeGetSceneSummaries(argumentsJson);
                        break;
                    case "generate_script_outline":
                        result = executeGenerateScriptOutline(argumentsJson);
                        break;
                    case "update_script":
                        result = executeUpdateScript(argumentsJson);
                        break;
                    case "update_script_outline":
                        result = executeUpdateScriptOutline(argumentsJson);
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

    private String executeCreateScript(String argumentsJson, GenreSession session) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
            String title = (String) args.get("title");
            String scriptType = (String) args.get("scriptType");
            String description = (String) args.get("description");

            Script script = new Script();
            script.setSessionId(session.getSessionId());
            script.setTitle(title);
            script.setScriptType(scriptType);
            script.setDescription(description);
            script = scriptRepository.save(script);

            // 更新会话标题
            updateSessionTitleIfNeeded(session, title);

            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "scriptId", script.getId(),
                "message", "剧本《" + title + "》创建成功"
            ));
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String executeAddScene(String argumentsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
            Long scriptId = Long.valueOf(args.get("scriptId").toString());
            Integer sceneNumber = Integer.valueOf(args.get("sceneNumber").toString());
            String location = (String) args.get("location");
            String time = (String) args.get("time");
            String content = (String) args.get("content");
            String summary = (String) args.get("summary");

            ScriptScene scene = new ScriptScene();
            scene.setScriptId(scriptId);
            scene.setSceneNumber(sceneNumber);
            scene.setLocation(location);
            scene.setTime(time);
            scene.setContent(content);
            scene.setSummary(summary);
            scene = scriptSceneRepository.save(scene);

            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "sceneId", scene.getId(),
                "message", "场景" + sceneNumber + "添加成功"
            ));
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String executeUpdateScene(String argumentsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
            Long sceneId = Long.valueOf(args.get("sceneId").toString());
            String content = (String) args.get("content");
            String summary = (String) args.get("summary");

            ScriptScene scene = scriptSceneRepository.findById(sceneId)
                .orElseThrow(() -> new RuntimeException("场景不存在: " + sceneId));
            scene.setContent(content);
            if (summary != null) scene.setSummary(summary);
            scriptSceneRepository.save(scene);

            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "message", "场景更新成功"
            ));
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String executeGetSceneSummaries(String argumentsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
            Long scriptId = Long.valueOf(args.get("scriptId").toString());

            List<ScriptScene> scenes = scriptSceneRepository.findByScriptIdOrderBySceneNumberAsc(scriptId);
            List<Map<String, Object>> summaries = new ArrayList<>();
            for (ScriptScene scene : scenes) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("sceneId", scene.getId());
                item.put("sceneNumber", scene.getSceneNumber());
                item.put("location", scene.getLocation());
                item.put("summary", scene.getSummary() != null ? scene.getSummary() :
                    scene.getContent().substring(0, Math.min(100, scene.getContent().length())));
                summaries.add(item);
            }

            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "scenes", summaries
            ));
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String executeGenerateScriptOutline(String argumentsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
            Long scriptId = Long.valueOf(args.get("scriptId").toString());
            String outline = (String) args.get("outline");

            Script script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("剧本不存在: " + scriptId));
            script.setOutline(outline);
            scriptRepository.save(script);

            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "message", "大纲生成成功"
            ));
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String executeUpdateScript(String argumentsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
            Long scriptId = Long.valueOf(args.get("scriptId").toString());

            Script script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("剧本不存在: " + scriptId));

            if (args.containsKey("title")) script.setTitle((String) args.get("title"));
            if (args.containsKey("description")) script.setDescription((String) args.get("description"));
            if (args.containsKey("scriptType")) script.setScriptType((String) args.get("scriptType"));
            scriptRepository.save(script);

            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "message", "剧本信息更新成功"
            ));
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String executeUpdateScriptOutline(String argumentsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
            Long scriptId = Long.valueOf(args.get("scriptId").toString());
            String outline = (String) args.get("outline");

            Script script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("剧本不存在: " + scriptId));
            script.setOutline(outline);
            scriptRepository.save(script);

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
        params.put("scriptType", null);
        params.put("theme", null);
        params.put("style", null);
        params.put("structure", null);
        params.put("dialogueStyle", null);
        params.put("actCount", 3);
        return params;
    }
}

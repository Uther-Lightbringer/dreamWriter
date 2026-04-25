package org.zenithon.articlecollect.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.zenithon.articlecollect.config.DeepSeekConfig;
import org.zenithon.articlecollect.dto.NovelGeneratorRequest;
import org.zenithon.articlecollect.entity.CreativeSession;
import org.zenithon.articlecollect.repository.CreativeSessionRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 创作引导会话服务
 *
 * 遵循 DeepSeek API 规范（docs/deepseek-api-reference.md）：
 * 1. 每次请求携带完整 messages 历史
 * 2. 使用 strict 模式定义工具
 * 3. 处理 reasoning_content 作为备选响应
 * 4. 使用 SSE 流式输出
 */
@Service
public class CreativeSessionService {

    private static final Logger logger = LoggerFactory.getLogger(CreativeSessionService.class);

    // 用于清理思考标签的正则表达式
    private static final Pattern THINK_TAG_PATTERN = Pattern.compile("<think>.*?</think>", Pattern.DOTALL);

    // 系统提示词
    private static final String SYSTEM_PROMPT = buildSystemPrompt();

    private final CreativeSessionRepository sessionRepository;
    private final DeepSeekConfig deepSeekConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public CreativeSessionService(
            CreativeSessionRepository sessionRepository,
            DeepSeekConfig deepSeekConfig,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.deepSeekConfig = deepSeekConfig;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    // ==================== 会话管理 ====================

    /**
     * 创建新会话
     */
    @Transactional
    public CreativeSession createSession(String title) {
        String sessionId = "sess_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        CreativeSession session = new CreativeSession(sessionId);
        if (title != null && !title.trim().isEmpty()) {
            session.setTitle(title);
        } else {
            session.setTitle("新创作会话 " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd HH:mm")));
        }

        // 初始化空的消息历史（包含系统提示词）
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        session.setMessages(toJson(messages));

        // 初始化空的参数
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("theme", null);
        params.put("style", null);
        params.put("protagonistName", null);
        params.put("protagonistGender", null);
        params.put("protagonistIdentity", null);
        params.put("mainPlot", null);
        params.put("conflictType", null);
        params.put("endingType", null);
        params.put("chapterCount", 8);
        params.put("wordsPerChapter", 3000);
        params.put("pointOfView", null);
        params.put("otherCharacters", new ArrayList<>());
        params.put("languageStyle", null);
        session.setExtractedParams(toJson(params));

        return sessionRepository.save(session);
    }

    /**
     * 获取所有会话列表
     */
    public List<CreativeSession> getAllSessions() {
        return sessionRepository.findAllByOrderByUpdateTimeDesc();
    }

    /**
     * 获取会话详情
     */
    public CreativeSession getSession(String sessionId) {
        return sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("会话不存在: " + sessionId));
    }

    /**
     * 更新会话
     */
    @Transactional
    public CreativeSession updateSession(String sessionId, String title, CreativeSession.SessionStatus status) {
        CreativeSession session = getSession(sessionId);
        if (title != null && !title.trim().isEmpty()) {
            session.setTitle(title);
        }
        if (status != null) {
            session.setStatus(status);
        }
        return sessionRepository.save(session);
    }

    /**
     * 删除会话
     */
    @Transactional
    public void deleteSession(String sessionId) {
        sessionRepository.deleteBySessionId(sessionId);
    }

    // ==================== 对话功能 ====================

    /**
     * 发送消息并获取流式响应
     */
    @Transactional
    public void chat(String sessionId, String content, SseEmitter emitter) {
        // 检查 API Key
        if (deepSeekConfig.getApiKey() == null || deepSeekConfig.getApiKey().trim().isEmpty()) {
            sendError(emitter, "DeepSeek API Key 未配置");
            return;
        }

        CreativeSession session = getSession(sessionId);

        try {
            // 解析现有消息历史
            List<Map<String, Object>> messages = parseMessages(session.getMessages());

            // 添加用户消息
            messages.add(Map.of("role", "user", "content", content));

            // 调用 DeepSeek API（流式）
            chatStream(messages, emitter, session);

        } catch (Exception e) {
            logger.error("对话失败: {}", e.getMessage(), e);
            sendError(emitter, "对话失败: " + e.getMessage());
        }
    }

    /**
     * 流式调用 DeepSeek API（真正的流式处理）
     */
    private void chatStream(List<Map<String, Object>> messages, SseEmitter emitter, CreativeSession session) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + deepSeekConfig.getApiKey());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", deepSeekConfig.getModel());
            requestBody.put("messages", messages);
            requestBody.put("stream", true);
            requestBody.put("max_tokens", 2000);

            // 添加工具定义
            requestBody.put("tools", getGuidanceTools());

            String requestBodyStr = objectMapper.writeValueAsString(requestBody);

            logger.info("调用 DeepSeek API (流式), model={}, messages={}", deepSeekConfig.getModel(), messages.size());

            // 使用 RestTemplate.execute 实现真正的流式请求
            restTemplate.execute(
                    deepSeekConfig.getApiUrl(),
                    org.springframework.http.HttpMethod.POST,
                    request -> {
                        request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                        request.getHeaders().set("Authorization", "Bearer " + deepSeekConfig.getApiKey());
                        request.getBody().write(requestBodyStr.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    },
                    response -> {
                        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                                new java.io.InputStreamReader(response.getBody(), java.nio.charset.StandardCharsets.UTF_8))) {

                            StringBuilder fullContent = new StringBuilder();
                            List<Map<String, Object>> toolCalls = new ArrayList<>();
                            String line;

                            while ((line = reader.readLine()) != null) {
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

                                            // 处理内容
                                            JsonNode contentNode = delta.get("content");
                                            if (contentNode != null && !contentNode.isNull() && contentNode.isTextual()) {
                                                String content = contentNode.asText();
                                                if (content != null && !content.isEmpty()) {
                                                    fullContent.append(content);
                                                    // 实时发送内容事件
                                                    emitter.send(SseEmitter.event()
                                                            .name("content")
                                                            .data(objectMapper.writeValueAsString(Map.of("text", content))));
                                                }
                                            }

                                            // 处理工具调用
                                            JsonNode toolCallsDelta = delta.path("tool_calls");
                                            if (toolCallsDelta.isArray() && toolCallsDelta.size() > 0) {
                                                for (JsonNode tc : toolCallsDelta) {
                                                    String toolCallId = tc.path("id").asText();
                                                    JsonNode function = tc.path("function");
                                                    String functionName = function.path("name").asText();
                                                    String arguments = function.path("arguments").asText();

                                                    Map<String, Object> toolCall = new LinkedHashMap<>();
                                                    toolCall.put("id", toolCallId);
                                                    toolCall.put("type", "function");
                                                    Map<String, Object> func = new LinkedHashMap<>();
                                                    func.put("name", functionName);
                                                    func.put("arguments", arguments);
                                                    toolCall.put("function", func);
                                                    toolCalls.add(toolCall);
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        logger.warn("解析 SSE 数据块失败: {}", e.getMessage());
                                    }
                                }
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
                            }

                            // 更新会话消息历史
                            session.setMessages(toJson(messages));
                            sessionRepository.save(session);

                            // 发送完成事件
                            emitter.send(SseEmitter.event().name("done").data("{}"));
                            emitter.complete();

                        } catch (Exception e) {
                            logger.error("处理流式响应失败: {}", e.getMessage(), e);
                            sendError(emitter, "处理响应失败: " + e.getMessage());
                        }
                        return null;
                    }
            );

        } catch (Exception e) {
            logger.error("流式调用失败: {}", e.getMessage(), e);
            sendError(emitter, "流式调用失败: " + e.getMessage());
        }
    }

    /**
     * 处理工具调用
     */
    @SuppressWarnings("unchecked")
    private void processToolCalls(List<Map<String, Object>> toolCalls, SseEmitter emitter,
                                  CreativeSession session, List<Map<String, Object>> messages) {
        try {
            for (Map<String, Object> toolCall : toolCalls) {
                String toolCallId = (String) toolCall.get("id");
                Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
                String functionName = (String) function.get("name");
                String argumentsJson = (String) function.get("arguments");

                logger.info("处理工具调用: {}({})", functionName, argumentsJson);

                String result = "";

                switch (functionName) {
                    case "preview_params":
                        result = previewParams(session);
                        break;
                    case "fill_params":
                        result = fillParamsInternal(session, argumentsJson);
                        break;
                    case "show_examples":
                        result = showExamples(argumentsJson);
                        break;
                    default:
                        result = "{\"error\": \"未知工具: " + functionName + "\"}";
                }

                // 添加工具结果到消息历史
                messages.add(Map.of(
                        "role", "tool",
                        "tool_call_id", toolCallId,
                        "content", result
                ));

                // 发送工具结果事件
                emitter.send(SseEmitter.event()
                        .name("tool_result")
                        .data(objectMapper.writeValueAsString(Map.of(
                                "tool", functionName,
                                "result", result
                        ))));
            }
        } catch (Exception e) {
            logger.error("处理工具调用失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 预览参数
     */
    private String previewParams(CreativeSession session) {
        try {
            Map<String, Object> params = parseParams(session.getExtractedParams());
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 填充参数（公开方法，用于前端调用）
     */
    @Transactional
    public NovelGeneratorRequest fillParams(String sessionId) {
        CreativeSession session = getSession(sessionId);
        return getGeneratorRequest(session);
    }

    /**
     * 填充参数（内部方法）
     */
    private String fillParamsInternal(CreativeSession session, String argumentsJson) {
        try {
            Map<String, Object> params = parseParams(session.getExtractedParams());

            // 如果有传入的参数，合并
            if (argumentsJson != null && !argumentsJson.isEmpty()) {
                Map<String, Object> newParams = objectMapper.readValue(argumentsJson,
                        new TypeReference<Map<String, Object>>() {});
                params.putAll(newParams);
            }

            // 更新会话参数
            session.setExtractedParams(toJson(params));
            sessionRepository.save(session);

            return objectMapper.writeValueAsString(Map.of("success", true, "params", params));
        } catch (Exception e) {
            logger.error("填充参数失败: {}", e.getMessage(), e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 展示示例
     */
    private String showExamples(String argumentsJson) {
        // TODO: 实现示例展示功能
        return "{\"message\": \"示例功能暂未实现\"}";
    }

    /**
     * 获取创作参数（用于前端填充表单）
     */
    public NovelGeneratorRequest getGeneratorRequest(String sessionId) {
        CreativeSession session = getSession(sessionId);
        return getGeneratorRequest(session);
    }

    /**
     * 获取创作参数（内部方法）
     */
    @SuppressWarnings("unchecked")
    private NovelGeneratorRequest getGeneratorRequest(CreativeSession session) {
        Map<String, Object> params = parseParams(session.getExtractedParams());

        NovelGeneratorRequest request = new NovelGeneratorRequest();

        // 构建 keyword（故事大纲）
        StringBuilder keyword = new StringBuilder();
        if (params.get("theme") != null) {
            keyword.append("题材：").append(params.get("theme")).append("。");
        }
        if (params.get("style") != null) {
            keyword.append("风格：").append(params.get("style")).append("。");
        }
        if (params.get("protagonistName") != null) {
            keyword.append("主角：").append(params.get("protagonistName"));
            if (params.get("protagonistGender") != null) {
                keyword.append("（").append(params.get("protagonistGender")).append("）");
            }
            if (params.get("protagonistIdentity") != null) {
                keyword.append("，身份：").append(params.get("protagonistIdentity"));
            }
            keyword.append("。");
        }
        if (params.get("mainPlot") != null) {
            keyword.append("主线：").append(params.get("mainPlot")).append("。");
        }
        if (params.get("conflictType") != null) {
            keyword.append("冲突：").append(params.get("conflictType")).append("。");
        }
        if (params.get("endingType") != null) {
            keyword.append("结局：").append(params.get("endingType")).append("。");
        }

        request.setKeyword(keyword.toString());

        // 设置其他参数
        if (params.get("chapterCount") != null) {
            request.setChapterCount(((Number) params.get("chapterCount")).intValue());
        }
        if (params.get("wordsPerChapter") != null) {
            request.setWordsPerChapter(((Number) params.get("wordsPerChapter")).intValue());
        }
        if (params.get("genre") != null || params.get("theme") != null) {
            request.setGenre(params.get("genre") != null ? (String) params.get("genre") : (String) params.get("theme"));
        }
        if (params.get("protagonistName") != null) {
            request.setProtagonist((String) params.get("protagonistName"));
        }
        if (params.get("languageStyle") != null) {
            request.setLanguageStyle((String) params.get("languageStyle"));
        }
        if (params.get("pointOfView") != null) {
            request.setPointOfView((String) params.get("pointOfView"));
        }

        return request;
    }

    // ==================== 工具定义 ====================

    /**
     * 获取引导工具定义
     */
    private List<Map<String, Object>> getGuidanceTools() {
        List<Map<String, Object>> tools = new ArrayList<>();

        // preview_params 工具
        Map<String, Object> previewParamsFunc = new LinkedHashMap<>();
        previewParamsFunc.put("name", "preview_params");
        previewParamsFunc.put("strict", true);
        previewParamsFunc.put("description", "展示当前对话中已提取的所有创作参数");
        previewParamsFunc.put("parameters", Map.of(
                "type", "object",
                "properties", Map.of(),
                "additionalProperties", false
        ));

        tools.add(Map.of(
                "type", "function",
                "function", previewParamsFunc
        ));

        // fill_params 工具
        Map<String, Object> fillParamsParams = new LinkedHashMap<>();
        fillParamsParams.put("type", "object");
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("chapterCount", Map.of("type", "integer", "description", "章节数量"));
        properties.put("keyword", Map.of("type", "string", "description", "故事大纲或关键词"));
        properties.put("genre", Map.of("type", "string", "description", "文学体裁"));
        properties.put("protagonist", Map.of("type", "string", "description", "主角姓名"));
        properties.put("languageStyle", Map.of("type", "string", "description", "语言风格"));
        properties.put("wordsPerChapter", Map.of("type", "integer", "description", "每章字数"));
        properties.put("pointOfView", Map.of("type", "string", "description", "叙述视角"));
        fillParamsParams.put("properties", properties);
        fillParamsParams.put("required", Arrays.asList("chapterCount", "keyword"));
        fillParamsParams.put("additionalProperties", false);

        Map<String, Object> fillParamsFunc = new LinkedHashMap<>();
        fillParamsFunc.put("name", "fill_params");
        fillParamsFunc.put("strict", true);
        fillParamsFunc.put("description", "将对话中确认的创作参数填充到梦境创作表单");
        fillParamsFunc.put("parameters", fillParamsParams);

        tools.add(Map.of(
                "type", "function",
                "function", fillParamsFunc
        ));

        // show_examples 工具
        Map<String, Object> showExamplesParams = new LinkedHashMap<>();
        showExamplesParams.put("type", "object");
        showExamplesParams.put("properties", Map.of(
                "genre", Map.of("type", "string", "description", "题材类型")
        ));
        showExamplesParams.put("required", Arrays.asList("genre"));
        showExamplesParams.put("additionalProperties", false);

        Map<String, Object> showExamplesFunc = new LinkedHashMap<>();
        showExamplesFunc.put("name", "show_examples");
        showExamplesFunc.put("strict", true);
        showExamplesFunc.put("description", "根据题材类型，展示相似题材的优秀作品片段作为参考");
        showExamplesFunc.put("parameters", showExamplesParams);

        tools.add(Map.of(
                "type", "function",
                "function", showExamplesFunc
        ));

        return tools;
    }

    // ==================== 系统提示词 ====================

    /**
     * 构建系统提示词
     */
    private static String buildSystemPrompt() {
        return """
            你是一位专业的小说创作顾问。你的任务是通过友好的对话，引导用户逐步完善创作设定。

            ## 你的工作方式

            1. **主动引导**：每次只问 1-2 个相关问题，不要一次性问太多
            2. **记住已确认的内容**：不要重复问用户已经回答过的问题
            3. **及时确认**：每完成一个阶段，简要总结并询问是否正确
            4. **灵活应变**：用户可能跳过某些问题或主动提供信息，要能适应
            5. **鼓励创作**：对用户的想法给予积极反馈和建议
            6. **提供选项**：每次提问都提供 4-6 个常见选项，同时允许用户自定义输入

            ## 引导顺序

            按以下顺序引导，但可根据对话自然调整：

            1. 题材和风格
            2. 主角设定（姓名、性别、身份）
            3. 故事主线（主线剧情、核心冲突、结局类型）
            4. 细节参数（章节数、字数、视角等）

            ## 回复格式

            你的回复需要通过特殊标记来指示选项和参数更新：

            1. 普通回复文字直接输出
            2. 选项列表使用以下格式：
            ```
            [OPTIONS:field_name]
            选项1|选项2|选项3|选项4|其他:请输入自定义内容
            [/OPTIONS]
            ```
            3. 参数更新使用以下格式：
            ```
            [PARAM:field_name=value]
            ```

            ## 各阶段选项参考

            ### 题材选项
            古代宫廷、现代都市、玄幻修仙、科幻未来、悬疑推理、民国谍战、奇幻冒险、其他题材

            ### 风格选项
            暗黑复仇、甜蜜言情、宫斗权谋、轻松搞笑、虐心催泪、热血励志、其他风格

            ### 性别选项
            男性、女性、双主角

            ### 结局选项
            圆满结局、悲剧结局、开放式结局、复仇成功、逆袭成功

            ### 视角选项
            第一人称视角、第三人称全知视角、第三人称有限视角、第二人称视角

            ## 特殊操作

            当用户点击「生成参数」按钮时，系统会发送：
            ```
            [SYSTEM: 用户请求生成参数，请输出 fill_params 工具调用]
            ```

            此时你需要调用 fill_params 工具，将所有已确定的参数填充到表单。
            """;
    }

    // ==================== 辅助方法 ====================

    /**
     * 解析消息历史
     */
    private List<Map<String, Object>> parseMessages(String messagesJson) {
        if (messagesJson == null || messagesJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(messagesJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (JsonProcessingException e) {
            logger.error("解析消息历史失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 解析参数
     */
    private Map<String, Object> parseParams(String paramsJson) {
        if (paramsJson == null || paramsJson.isEmpty()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(paramsJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            logger.error("解析参数失败: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    /**
     * 转换为 JSON 字符串
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("JSON 序列化失败: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * 发送错误事件
     */
    private void sendError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(objectMapper.writeValueAsString(Map.of("message", message))));
            emitter.complete();
        } catch (Exception e) {
            logger.error("发送错误事件失败: {}", e.getMessage());
        }
    }
}

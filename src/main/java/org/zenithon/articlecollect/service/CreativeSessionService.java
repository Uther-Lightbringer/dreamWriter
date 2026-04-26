package org.zenithon.articlecollect.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.zenithon.articlecollect.config.DeepSeekConfig;
import org.zenithon.articlecollect.dto.DeepSeekConfigDTO;
import org.zenithon.articlecollect.dto.DeepSeekRuntimeConfig;
import org.zenithon.articlecollect.dto.NovelGeneratorRequest;
import org.zenithon.articlecollect.entity.CreativeMemory;
import org.zenithon.articlecollect.entity.CreativeSession;
import org.zenithon.articlecollect.entity.DeepSeekFeatureConfig.FeatureCode;
import org.zenithon.articlecollect.repository.CreativeMemoryRepository;
import org.zenithon.articlecollect.repository.CreativeSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.zenithon.articlecollect.dto.CharacterCard;
import org.zenithon.articlecollect.entity.Chapter;
import org.zenithon.articlecollect.entity.Novel;

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

    // 对话轮数阈值，超过此值触发摘要
    private static final int SUMMARY_THRESHOLD = 20;
    // 保留最近对话轮数
    private static final int KEEP_RECENT_TURNS = 10;
    // 工具调用最大递归深度，防止无限递归
    private static final int MAX_TOOL_RECURSION_DEPTH = 5;

    // 工具名称中文映射
    private static final Map<String, String> TOOL_NAME_MAP = Map.ofEntries(
        Map.entry("create_novel", "创建小说"),
        Map.entry("add_chapter", "添加章节"),
        Map.entry("create_character_card", "创建角色卡"),
        Map.entry("update_character_card", "更新角色卡"),
        Map.entry("generate_character_image", "生成角色图片"),
        Map.entry("generate_chapter_images", "生成章节配图"),
        Map.entry("fill_params", "更新参数"),
        Map.entry("add_memory", "保存偏好"),
        Map.entry("preview_params", "预览参数"),
        Map.entry("remove_memory", "删除记忆"),
        Map.entry("show_examples", "展示示例")
    );

    // 系统提示词（固定部分）
    private static final String SYSTEM_PROMPT_BASE = buildSystemPromptBase();

    private final CreativeSessionRepository sessionRepository;
    private final CreativeMemoryRepository memoryRepository;
    private final DeepSeekConfig deepSeekConfig;
    private final DeepSeekConfigService deepSeekConfigService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    private NovelService novelService;

    @Autowired
    private CharacterCardService characterCardService;

    @Autowired
    private EvoLinkImageService evoLinkImageService;

    @Autowired
    private ChapterImageService chapterImageService;

    public CreativeSessionService(
            CreativeSessionRepository sessionRepository,
            CreativeMemoryRepository memoryRepository,
            DeepSeekConfig deepSeekConfig,
            DeepSeekConfigService deepSeekConfigService,
            RestTemplate restTemplate,
            ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.memoryRepository = memoryRepository;
        this.deepSeekConfig = deepSeekConfig;
        this.deepSeekConfigService = deepSeekConfigService;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    // ==================== 会话上下文管理 ====================

    /**
     * 会话上下文数据结构
     */
    public static class SessionContext {
        private Long currentNovelId;
        private Long currentChapterId;
        private List<Long> createdCharacterIds = new ArrayList<>();

        public Long getCurrentNovelId() { return currentNovelId; }
        public void setCurrentNovelId(Long novelId) { this.currentNovelId = novelId; }

        public Long getCurrentChapterId() { return currentChapterId; }
        public void setCurrentChapterId(Long chapterId) { this.currentChapterId = chapterId; }

        public List<Long> getCreatedCharacterIds() { return createdCharacterIds; }
        public void addCharacterId(Long id) { this.createdCharacterIds.add(id); }
    }

    /**
     * 获取会话上下文
     */
    private SessionContext getContext(CreativeSession session) {
        if (session.getContextData() == null || session.getContextData().isEmpty()) {
            return new SessionContext();
        }
        try {
            return objectMapper.readValue(session.getContextData(), SessionContext.class);
        } catch (Exception e) {
            logger.error("解析上下文失败: {}", e.getMessage());
            return new SessionContext();
        }
    }

    /**
     * 更新会话上下文
     */
    private void updateContext(CreativeSession session, SessionContext context) {
        try {
            session.setContextData(objectMapper.writeValueAsString(context));
            sessionRepository.save(session);
        } catch (Exception e) {
            logger.error("保存上下文失败: {}", e.getMessage());
        }
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

        // 初始化消息历史（包含系统提示词 + 用户记忆）
        List<Map<String, Object>> messages = new ArrayList<>();
        String systemPrompt = buildSystemPromptWithMemories(sessionId);
        messages.add(Map.of("role", "system", "content", systemPrompt));
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

        // 如果会话已绑定小说，拒绝修改标题
        if (title != null && !title.trim().isEmpty()) {
            SessionContext context = getContext(session);
            if (context.getCurrentNovelId() != null) {
                logger.warn("会话已绑定小说，拒绝修改标题: sessionId={}", sessionId);
                // 忽略标题修改，继续处理其他字段
            } else {
                session.setTitle(title);
            }
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
     * 发送消息并获取流式响应（使用默认配置）
     */
    @Transactional
    public void chat(String sessionId, String content, SseEmitter emitter) {
        chat(sessionId, content, emitter, null);
    }

    /**
     * 发送消息并获取流式响应（支持运行时配置覆盖）
     */
    @Transactional
    public void chat(String sessionId, String content, SseEmitter emitter, DeepSeekConfigDTO runtimeConfig) {
        // 检查 API Key
        if (deepSeekConfig.getApiKey() == null || deepSeekConfig.getApiKey().trim().isEmpty()) {
            sendError(emitter, "DeepSeek API Key 未配置");
            return;
        }

        CreativeSession session = getSession(sessionId);

        try {
            // 解析现有消息历史
            List<Map<String, Object>> messages = parseMessages(session.getMessages());

            // 刷新系统消息中的记忆部分（每次请求都更新）
            refreshMemoriesInSystemMessage(messages, sessionId);

            // 添加用户消息
            messages.add(Map.of("role", "user", "content", content));

            // 如果是第一条用户消息，使用前7个字作为临时标题
            if (countUserTurns(messages) == 1 && content != null && !content.isEmpty()) {
                String tempTitle = content.length() > 7
                    ? content.substring(0, 7) + "..."
                    : content;
                session.setTitle(tempTitle);
                logger.info("设置临时会话标题: {}", tempTitle);
            }

            // 检查是否需要生成摘要
            if (countUserTurns(messages) > SUMMARY_THRESHOLD) {
                int turnsSummarized = countUserTurns(messages) - KEEP_RECENT_TURNS;
                generateAndInsertSummary(messages, emitter, turnsSummarized);
            }

            // 获取运行时配置
            DeepSeekRuntimeConfig config = deepSeekConfigService.getRuntimeConfig(FeatureCode.CREATIVE_GUIDANCE, runtimeConfig);

            // 调用 DeepSeek API（流式）
            chatStream(messages, emitter, session, config);

        } catch (Exception e) {
            logger.error("对话失败: {}", e.getMessage(), e);
            sendError(emitter, "对话失败: " + e.getMessage());
        }
    }

    /**
     * 构建聊天请求体（公共逻辑提取）
     *
     * @param messages 消息历史
     * @param config 运行时配置
     * @return 请求体 Map
     */
    private Map<String, Object> buildChatRequestBody(List<Map<String, Object>> messages, DeepSeekRuntimeConfig config) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", config.getModel());
        requestBody.put("messages", messages);
        requestBody.put("stream", true);
        requestBody.put("stream_options", Map.of("include_usage", true));
        requestBody.put("max_tokens", deepSeekConfig.getMaxTokens());
        requestBody.put("tools", getGuidanceTools());

        // 思考模式配置
        if (config.getThinkingEnabled()) {
            Map<String, Object> thinking = new LinkedHashMap<>();
            thinking.put("type", "enabled");
            requestBody.put("thinking", thinking);
            requestBody.put("reasoning_effort", config.getReasoningEffort());
        }

        return requestBody;
    }

    /**
     * 流式调用 DeepSeek API（真正的流式处理）
     */
    private void chatStream(List<Map<String, Object>> messages, SseEmitter emitter, CreativeSession session, DeepSeekRuntimeConfig config) {
        try {
            // 构建请求体
            Map<String, Object> requestBody = buildChatRequestBody(messages, config);
            String requestBodyStr = objectMapper.writeValueAsString(requestBody);

            logger.info("调用 DeepSeek API (流式), model={}, thinking={}, messages={}",
                    config.getModel(), config.getThinkingEnabled(), messages.size());

            // 调试：打印完整的消息内容
            logger.info("请求消息内容:\n{}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(messages));

            // 检查重复的 tool_call_id
            Set<String> allToolCallIds = new HashSet<>();
            for (Map<String, Object> msg : messages) {
                if ("assistant".equals(msg.get("role"))) {
                    List<Map<String, Object>> tcs = (List<Map<String, Object>>) msg.get("tool_calls");
                    if (tcs != null) {
                        for (Map<String, Object> tc : tcs) {
                            String id = (String) tc.get("id");
                            if (id != null && !id.isEmpty()) {
                                if (allToolCallIds.contains(id)) {
                                    logger.error("发现重复的 tool_call_id: {} 在 assistant 消息中!", id);
                                }
                                allToolCallIds.add(id);
                            }
                        }
                    }
                }
            }
            if (!allToolCallIds.isEmpty()) {
                logger.info("所有 tool_call_id: {}", allToolCallIds);
            }

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
                            StringBuilder reasoningContent = new StringBuilder();  // 存储推理内容
                            // 使用 Map 按 toolCallId 累积工具调用
                            Map<String, Map<String, Object>> toolCallsMap = new LinkedHashMap<>();
                            Map<String, Object> usageInfo = new LinkedHashMap<>();  // 存储 usage 信息
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

                                        // 提取 usage 信息（通常在最后一个 chunk 中）
                                        JsonNode usageNode = jsonNode.get("usage");
                                        if (usageNode != null && !usageNode.isNull()) {
                                            usageInfo.clear();
                                            usageNode.fields().forEachRemaining(entry -> {
                                                usageInfo.put(entry.getKey(), entry.getValue().asInt());
                                            });
                                        }

                                        if (choices.isArray() && choices.size() > 0) {
                                            JsonNode delta = choices.get(0).path("delta");

                                            // 处理推理内容（reasoning_content）
                                            JsonNode reasoningNode = delta.get("reasoning_content");
                                            if (reasoningNode != null && !reasoningNode.isNull() && reasoningNode.isTextual()) {
                                                reasoningContent.append(reasoningNode.asText());
                                            }

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

                                            // 处理工具调用（累积模式）
                                            // 重要：DeepSeek 流式响应使用 index 标识同一个工具调用的不同 chunk
                                            JsonNode toolCallsDelta = delta.path("tool_calls");
                                            if (toolCallsDelta.isArray() && toolCallsDelta.size() > 0) {
                                                for (JsonNode tc : toolCallsDelta) {
                                                    // 使用 index 作为累积 key（DeepSeek 流式规范）
                                                    int index = tc.path("index").asInt(-1);
                                                    String toolCallId = tc.path("id").asText(null);
                                                    JsonNode function = tc.path("function");
                                                    String functionName = function.path("name").asText(null);
                                                    String argumentsChunk = function.path("arguments").asText(null);

                                                    // 使用 index 作为 mapKey，确保同一工具调用的 chunk 正确累积
                                                    String mapKey = (index >= 0) ? String.valueOf(index) : "_default_";

                                                    Map<String, Object> toolCall = toolCallsMap.computeIfAbsent(
                                                            mapKey,
                                                            k -> {
                                                                Map<String, Object> tc2 = new LinkedHashMap<>();
                                                                // 先用 index 作为临时 id，后续会被真实 id 覆盖
                                                                tc2.put("id", "pending_id_" + index);
                                                                tc2.put("type", "function");
                                                                tc2.put("index", index);
                                                                Map<String, Object> func = new LinkedHashMap<>();
                                                                func.put("name", "");
                                                                func.put("arguments", new StringBuilder());
                                                                tc2.put("function", func);
                                                                logger.info("创建工具调用条目: index={}, mapKey={}", index, mapKey);
                                                                return tc2;
                                                            }
                                                    );

                                                    // 更新 id（如果提供了）
                                                    if (toolCallId != null && !toolCallId.isEmpty()) {
                                                        toolCall.put("id", toolCallId);
                                                        logger.debug("工具调用 id 更新: index={} -> id={}", index, toolCallId);
                                                    }

                                                    Map<String, Object> func = (Map<String, Object>) toolCall.get("function");
                                                    // 更新 function name（如果有）
                                                    if (functionName != null && !functionName.isEmpty()) {
                                                        func.put("name", functionName);
                                                        logger.info("工具调用名称更新: index={}, name={}", index, functionName);
                                                    }
                                                    // 累积 arguments
                                                    if (argumentsChunk != null && !argumentsChunk.isEmpty()) {
                                                        ((StringBuilder) func.get("arguments")).append(argumentsChunk);
                                                        logger.debug("工具调用参数累积: index={}, +{} 字符", index, argumentsChunk.length());
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        logger.warn("解析 SSE 数据块失败: {}", e.getMessage());
                                    }
                                }
                            }

                            // 将累积的 toolCallsMap 转换为最终格式
                            List<Map<String, Object>> toolCalls = new ArrayList<>();
                            for (Map<String, Object> tc : toolCallsMap.values()) {
                                Map<String, Object> finalTc = new LinkedHashMap<>();
                                String finalId = (String) tc.get("id");
                                // 如果 id 仍是 pending_ 开头，生成最终 id
                                if (finalId == null || finalId.startsWith("pending_id_")) {
                                    finalId = "auto_id_" + UUID.randomUUID().toString().substring(0, 8);
                                }
                                finalTc.put("id", finalId);
                                finalTc.put("type", tc.get("type"));
                                Map<String, Object> func = (Map<String, Object>) tc.get("function");
                                Map<String, Object> finalFunc = new LinkedHashMap<>();
                                String funcName = (String) func.get("name");
                                finalFunc.put("name", funcName);
                                finalFunc.put("arguments", func.get("arguments").toString());  // StringBuilder 转 String
                                finalTc.put("function", finalFunc);
                                toolCalls.add(finalTc);

                                // 调试日志
                                logger.info("工具调用累积完成: id={}, name={}, arguments={}", finalId, funcName, finalFunc.get("arguments"));
                            }

                            // 构建助手消息
                            Map<String, Object> assistantMessage = new LinkedHashMap<>();
                            assistantMessage.put("role", "assistant");
                            assistantMessage.put("content", fullContent.toString());

                            // 如果有推理内容，必须包含在消息中（DeepSeek thinking 模式要求）
                            if (reasoningContent.length() > 0) {
                                assistantMessage.put("reasoning_content", reasoningContent.toString());
                            }

                            if (!toolCalls.isEmpty()) {
                                assistantMessage.put("tool_calls", toolCalls);
                            }
                            messages.add(assistantMessage);

                            // 处理工具调用
                            boolean hasToolCalls = !toolCalls.isEmpty();
                            if (hasToolCalls) {
                                processToolCalls(toolCalls, emitter, session, messages);

                                // 调试：打印 messages 验证 tool 消息
                                logger.info("=== 工具处理后消息验证 ===");
                                for (int i = 0; i < messages.size(); i++) {
                                    Map<String, Object> msg = messages.get(i);
                                    String role = (String) msg.get("role");
                                    if ("assistant".equals(role)) {
                                        List<Map<String, Object>> tcs = (List<Map<String, Object>>) msg.get("tool_calls");
                                        if (tcs != null) {
                                            List<String> ids = new ArrayList<>();
                                            for (Map<String, Object> tc : tcs) {
                                                ids.add((String) tc.get("id"));
                                            }
                                            logger.info("  [{}] assistant - tool_calls ids: {}", i, ids);
                                        } else {
                                            logger.info("  [{}] assistant (no tool_calls)", i);
                                        }
                                    } else if ("tool".equals(role)) {
                                        logger.info("  [{}] tool - tool_call_id: {}", i, msg.get("tool_call_id"));
                                    } else {
                                        String content = (String) msg.get("content");
                                        String preview = content != null && content.length() > 50
                                            ? content.substring(0, 50) + "..."
                                            : content;
                                        logger.info("  [{}] {} - {}", i, role, preview);
                                    }
                                }
                                logger.info("=== 消息验证结束 ===");

                                // 更新会话消息历史（包含工具结果）
                                session.setMessages(toJson(messages));
                                sessionRepository.save(session);

                                // 发送分隔事件，表示工具调用完成，即将继续生成
                                emitter.send(SseEmitter.event()
                                        .name("tool_calls_done")
                                        .data("{}"));

                                // 再次调用 API，让 AI 基于工具结果继续生成响应
                                logger.info("工具调用完成，再次调用 API 让 AI 继续生成响应");
                                continueAfterToolCalls(messages, emitter, session, config, 0);
                            } else {
                                // 没有工具调用，正常结束
                                session.setMessages(toJson(messages));
                                sessionRepository.save(session);

                                // 发送 usage 事件
                                if (!usageInfo.isEmpty()) {
                                    emitter.send(SseEmitter.event()
                                            .name("usage")
                                            .data(objectMapper.writeValueAsString(usageInfo)));
                                }

                                // 发送完成事件
                                emitter.send(SseEmitter.event().name("done").data("{}"));
                                emitter.complete();
                            }

                        } catch (Exception e) {
                            logger.error("处理流式响应失败: {}", e.getMessage(), e);
                            sendError(emitter, "处理响应失败: " + e.getMessage());
                        }
                        return null;
                    }
            );

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // 打印完整的 HTTP 错误响应
            String errorBody = e.getResponseBodyAsString();
            logger.error("流式调用失败: HTTP {} - {}", e.getStatusCode(), errorBody, e);
            sendError(emitter, "API 调用失败: " + errorBody);
        } catch (Exception e) {
            logger.error("流式调用失败: {}", e.getMessage(), e);
            sendError(emitter, "流式调用失败: " + e.getMessage());
        }
    }

    /**
     * 工具调用完成后继续生成响应（支持递归工具调用）
     *
     * @param messages 消息历史
     * @param emitter SSE 发射器
     * @param session 会话对象
     * @param config 运行时配置
     * @param recursionDepth 当前递归深度，用于防止无限递归
     */
    private void continueAfterToolCalls(List<Map<String, Object>> messages, SseEmitter emitter, CreativeSession session, DeepSeekRuntimeConfig config, int recursionDepth) {
        // 检查递归深度，防止栈溢出
        if (recursionDepth > MAX_TOOL_RECURSION_DEPTH) {
            logger.warn("工具调用递归深度超过限制: {}, 终止递归", recursionDepth);
            sendError(emitter, "工具调用次数过多，请简化操作");
            return;
        }

        try {
            // 构建请求体
            Map<String, Object> requestBody = buildChatRequestBody(messages, config);
            String requestBodyStr = objectMapper.writeValueAsString(requestBody);

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
                            StringBuilder reasoningContent = new StringBuilder();
                            // 使用 Map 按 toolCallId 累积工具调用（与 chatStream 相同逻辑）
                            Map<String, Map<String, Object>> toolCallsMap = new LinkedHashMap<>();
                            Map<String, Object> usageInfo = new LinkedHashMap<>();
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

                                        JsonNode usageNode = jsonNode.get("usage");
                                        if (usageNode != null && !usageNode.isNull()) {
                                            usageInfo.clear();
                                            usageNode.fields().forEachRemaining(entry -> {
                                                usageInfo.put(entry.getKey(), entry.getValue().asInt());
                                            });
                                        }

                                        if (choices.isArray() && choices.size() > 0) {
                                            JsonNode delta = choices.get(0).path("delta");

                                            JsonNode reasoningNode = delta.get("reasoning_content");
                                            if (reasoningNode != null && !reasoningNode.isNull() && reasoningNode.isTextual()) {
                                                reasoningContent.append(reasoningNode.asText());
                                            }

                                            JsonNode contentNode = delta.get("content");
                                            if (contentNode != null && !contentNode.isNull() && contentNode.isTextual()) {
                                                String content = contentNode.asText();
                                                if (content != null && !content.isEmpty()) {
                                                    fullContent.append(content);
                                                    emitter.send(SseEmitter.event()
                                                            .name("content")
                                                            .data(objectMapper.writeValueAsString(Map.of("text", content))));
                                                }
                                            }

                                            // 处理工具调用（累积模式，与 chatStream 相同）
                                            JsonNode toolCallsDelta = delta.path("tool_calls");
                                            if (toolCallsDelta.isArray() && toolCallsDelta.size() > 0) {
                                                for (JsonNode tc : toolCallsDelta) {
                                                    int index = tc.path("index").asInt(-1);
                                                    String toolCallId = tc.path("id").asText(null);
                                                    JsonNode function = tc.path("function");
                                                    String functionName = function.path("name").asText(null);
                                                    String argumentsChunk = function.path("arguments").asText(null);

                                                    String mapKey = (index >= 0) ? String.valueOf(index) : "_default_";

                                                    Map<String, Object> toolCall = toolCallsMap.computeIfAbsent(
                                                            mapKey,
                                                            k -> {
                                                                Map<String, Object> tc2 = new LinkedHashMap<>();
                                                                tc2.put("id", "pending_id_" + index);
                                                                tc2.put("type", "function");
                                                                tc2.put("index", index);
                                                                Map<String, Object> func = new LinkedHashMap<>();
                                                                func.put("name", "");
                                                                func.put("arguments", new StringBuilder());
                                                                tc2.put("function", func);
                                                                logger.info("递归工具调用累积: index={}, mapKey={}", index, mapKey);
                                                                return tc2;
                                                            }
                                                    );

                                                    if (toolCallId != null && !toolCallId.isEmpty()) {
                                                        toolCall.put("id", toolCallId);
                                                    }

                                                    Map<String, Object> func = (Map<String, Object>) toolCall.get("function");
                                                    if (functionName != null && !functionName.isEmpty()) {
                                                        func.put("name", functionName);
                                                        logger.info("递归工具调用名称: index={}, name={}", index, functionName);
                                                    }
                                                    if (argumentsChunk != null && !argumentsChunk.isEmpty()) {
                                                        ((StringBuilder) func.get("arguments")).append(argumentsChunk);
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        logger.warn("解析 SSE 数据块失败: {}", e.getMessage());
                                    }
                                }
                            }

                            // 将累积的 toolCallsMap 转换为最终格式
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
                                String funcName = (String) func.get("name");
                                finalFunc.put("name", funcName);
                                finalFunc.put("arguments", func.get("arguments").toString());
                                finalTc.put("function", finalFunc);
                                toolCalls.add(finalTc);

                                logger.info("递归工具调用完成: id={}, name={}, arguments={}", finalId, funcName, finalFunc.get("arguments"));
                            }

                            // 构建助手消息
                            Map<String, Object> assistantMessage = new LinkedHashMap<>();
                            assistantMessage.put("role", "assistant");
                            assistantMessage.put("content", fullContent.toString());
                            if (reasoningContent.length() > 0) {
                                assistantMessage.put("reasoning_content", reasoningContent.toString());
                            }

                            if (!toolCalls.isEmpty()) {
                                assistantMessage.put("tool_calls", toolCalls);
                            }
                            messages.add(assistantMessage);

                            // 处理递归工具调用
                            boolean hasToolCalls = !toolCalls.isEmpty();
                            if (hasToolCalls) {
                                processToolCalls(toolCalls, emitter, session, messages);

                                // 更新会话消息历史
                                session.setMessages(toJson(messages));
                                sessionRepository.save(session);

                                // 发送分隔事件
                                emitter.send(SseEmitter.event()
                                        .name("tool_calls_done")
                                        .data("{}"));

                                // 递归调用，支持连续工具调用
                                logger.info("递归工具调用检测到 {} 个工具，继续生成响应，当前深度: {}", toolCalls.size(), recursionDepth + 1);
                                continueAfterToolCalls(messages, emitter, session, config, recursionDepth + 1);
                            } else {
                                // 没有工具调用，正常结束
                                session.setMessages(toJson(messages));
                                sessionRepository.save(session);

                                // 发送 usage 和 done
                                if (!usageInfo.isEmpty()) {
                                    emitter.send(SseEmitter.event()
                                            .name("usage")
                                            .data(objectMapper.writeValueAsString(usageInfo)));
                                }

                                emitter.send(SseEmitter.event().name("done").data("{}"));
                                emitter.complete();
                            }

                        } catch (Exception e) {
                            logger.error("继续生成响应失败: {}", e.getMessage(), e);
                            sendError(emitter, "继续生成失败: " + e.getMessage());
                        }
                        return null;
                    }
            );

        } catch (Exception e) {
            logger.error("调用继续生成 API 失败: {}", e.getMessage(), e);
            sendError(emitter, "继续生成失败: " + e.getMessage());
        }
    }

    /**
     * 处理工具调用
     */
    @SuppressWarnings("unchecked")
    private void processToolCalls(List<Map<String, Object>> toolCalls, SseEmitter emitter,
                                  CreativeSession session, List<Map<String, Object>> messages) {
        try {
            // 发送工具调用开始事件
            for (Map<String, Object> toolCall : toolCalls) {
                Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
                String functionName = function != null ? (String) function.get("name") : "";
                String displayName = TOOL_NAME_MAP.getOrDefault(functionName, functionName);

                try {
                    emitter.send(SseEmitter.event()
                            .name("tool_call_start")
                            .data(objectMapper.writeValueAsString(Map.of(
                                    "tool", functionName,
                                    "displayName", displayName
                            ))));
                } catch (Exception e) {
                    logger.warn("发送工具调用开始事件失败: {}", e.getMessage());
                }
            }

            // 检查重复的 tool_call_id
            Set<String> seenIds = new HashSet<>();
            for (Map<String, Object> toolCall : toolCalls) {
                String toolCallId = (String) toolCall.get("id");

                // 如果 toolCallId 为空，生成一个默认 id
                if (toolCallId == null || toolCallId.isEmpty()) {
                    toolCallId = "auto_id_" + UUID.randomUUID().toString().substring(0, 8);
                    logger.info("为工具调用生成默认 id: {}", toolCallId);
                }

                if (seenIds.contains(toolCallId)) {
                    logger.warn("发现重复的 tool_call_id: {}, 跳过", toolCallId);
                    continue;
                }
                seenIds.add(toolCallId);

                Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
                String functionName = function != null ? (String) function.get("name") : "";
                String argumentsJson = function != null ? (String) function.get("arguments") : "{}";

                logger.info("处理工具调用: {}({})", functionName, argumentsJson);

                String result = "";

                switch (functionName) {
                    case "preview_params":
                        result = previewParams(session);
                        break;
                    case "fill_params":
                        result = fillParamsInternal(session, argumentsJson, messages);
                        break;
                    case "show_examples":
                        result = showExamples(argumentsJson);
                        break;
                    case "add_memory":
                        result = addMemory(session.getSessionId(), argumentsJson);
                        break;
                    case "remove_memory":
                        result = removeMemory(session.getSessionId(), argumentsJson);
                        break;
                    case "summarize":
                        result = summarizeConversation(messages);
                        break;
                    case "create_novel":
                        result = createNovel(argumentsJson, session, messages);
                        break;
                    case "add_chapter":
                        result = addChapter(argumentsJson, session, messages);
                        break;
                    case "create_character_card":
                        result = createCharacterCard(argumentsJson, session, messages);
                        break;
                    case "update_character_card":
                        result = updateCharacterCard(argumentsJson, session, messages);
                        break;
                    case "generate_character_image":
                        result = generateCharacterImage(argumentsJson, session, emitter);
                        break;
                    case "generate_chapter_images":
                        result = generateChapterImages(argumentsJson, session, emitter);
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

    // ==================== 工具实现 ====================

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
     * 填充参数（内部方法），并添加系统消息
     * 支持多选参数（数组格式）
     */
    private String fillParamsInternal(CreativeSession session, String argumentsJson, List<Map<String, Object>> messages) {
        try {
            Map<String, Object> params = parseParams(session.getExtractedParams());
            Map<String, Object> updatedFields = new LinkedHashMap<>();

            // 如果有传入的参数，合并
            if (argumentsJson != null && !argumentsJson.isEmpty()) {
                Map<String, Object> newParams = objectMapper.readValue(argumentsJson,
                        new TypeReference<Map<String, Object>>() {});

                for (Map.Entry<String, Object> entry : newParams.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (value != null) {
                        // 支持多选：值可能是 String 或 List
                        // Jackson 会自动解析 JSON 数组为 List
                        params.put(key, value);
                        updatedFields.put(key, value);
                    }
                }
            }

            // 更新会话参数
            session.setExtractedParams(toJson(params));
            sessionRepository.save(session);

            // 注意：不要在这里添加 system 消息到 messages
            // 因为 API 要求 tool 消息必须紧跟 assistant 消息

            return objectMapper.writeValueAsString(Map.of("success", true, "params", params, "updated", updatedFields));
        } catch (Exception e) {
            logger.error("填充参数失败: {}", e.getMessage(), e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 展示示例
     */
    private String showExamples(String argumentsJson) {
        return "{\"message\": \"示例功能暂未实现\"}";
    }

    /**
     * 添加记忆
     * 支持 scope 参数：global（全局记忆，默认）或 session（会话记忆）
     */
    @Transactional
    private String addMemory(String sessionId, String argumentsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<>() {});
            String key = (String) args.get("key");
            String value = (String) args.get("value");
            String scope = (String) args.getOrDefault("scope", "global"); // global 或 session

            if (key == null || value == null) {
                return "{\"error\": \"key 和 value 参数必填\"}";
            }

            boolean isSessionMemory = "session".equalsIgnoreCase(scope);
            String targetSessionId = isSessionMemory ? sessionId : null;

            // 检查是否已存在同 key 同 scope 的记忆
            Optional<CreativeMemory> existing;
            if (isSessionMemory) {
                existing = memoryRepository.findByKeyAndSessionId(key, sessionId);
            } else {
                existing = memoryRepository.findByKey(key)
                    .filter(m -> m.getSessionId() == null);
            }

            if (existing.isPresent()) {
                CreativeMemory memory = existing.get();
                memory.setValue(value);
                memory.setSourceSessionId(sessionId);
                memoryRepository.save(memory);
                logger.info("更新{}记忆: {} = {}", isSessionMemory ? "会话" : "全局", key, value);
            } else {
                CreativeMemory memory = new CreativeMemory(key, value, sessionId, isSessionMemory);
                memoryRepository.save(memory);
                logger.info("添加{}记忆: {} = {}", isSessionMemory ? "会话" : "全局", key, value);
            }

            String message = isSessionMemory ? "已添加到会话偏好" : "已添加到全局偏好";
            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "message", message,
                "key", key,
                "value", value,
                "scope", scope
            ));
        } catch (Exception e) {
            logger.error("添加记忆失败: {}", e.getMessage(), e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 删除记忆
     * 支持 scope 参数：global（全局记忆，默认）或 session（会话记忆）
     */
    @Transactional
    private String removeMemory(String sessionId, String argumentsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<>() {});
            String key = (String) args.get("key");
            String scope = (String) args.getOrDefault("scope", "global");

            if (key == null) {
                return "{\"error\": \"key 参数必填\"}";
            }

            boolean isSessionMemory = "session".equalsIgnoreCase(scope);

            if (isSessionMemory) {
                memoryRepository.deleteByKeyAndSessionId(key, sessionId);
                logger.info("删除会话记忆: {}, sessionId: {}", key, sessionId);
            } else {
                memoryRepository.deleteByKey(key);
                logger.info("删除全局记忆: {}", key);
            }

            String message = isSessionMemory ? "已删除会话记忆" : "已删除全局记忆";
            return objectMapper.writeValueAsString(Map.of("success", true, "message", message, "key", key, "scope", scope));
        } catch (Exception e) {
            logger.error("删除记忆失败: {}", e.getMessage(), e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 生成对话摘要
     */
    private String summarizeConversation(List<Map<String, Object>> messages) {
        // 简单实现：标记需要摘要，实际摘要由 AI 生成
        return "{\"message\": \"摘要功能将由 AI 在下次请求时生成\"}";
    }

    // ==================== 新增创作工具实现 ====================

    /**
     * 创建小说工具实现
     */
    private String createNovel(String argumentsJson, CreativeSession session, List<Map<String, Object>> messages) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<>() {});
            String title = (String) args.get("title");

            if (title == null || title.trim().isEmpty()) {
                return "{\"error\": \"小说标题不能为空\"}";
            }

            // 检查是否已绑定小说
            SessionContext context = getContext(session);
            if (context.getCurrentNovelId() != null) {
                Novel existingNovel = novelService.getNovelById(context.getCurrentNovelId());
                String existingTitle = existingNovel != null ? existingNovel.getTitle() : "未知";
                logger.warn("会话已绑定小说，拒绝重复创建: sessionId={}, existingNovelId={}", session.getSessionId(), context.getCurrentNovelId());
                return objectMapper.writeValueAsString(Map.of(
                    "success", false,
                    "error", "当前会话已绑定小说《" + existingTitle + "》，如需创建新小说请新开会话",
                    "errorCode", "NOVEL_ALREADY_BOUND"
                ));
            }

            // 调用 NovelService 创建小说
            Novel novel = novelService.createNovel(title.trim());

            // 更新会话上下文
            SessionContext context = getContext(session);
            context.setCurrentNovelId(novel.getId());
            updateContext(session, context);

            // 更新会话标题为小说标题
            session.setTitle(title.trim());
            sessionRepository.save(session);

            logger.info("创建小说成功: id={}, title={}", novel.getId(), title);

            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "novelId", novel.getId(),
                "title", title
            ));
        } catch (Exception e) {
            logger.error("创建小说失败: {}", e.getMessage(), e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 添加章节工具实现
     */
    private String addChapter(String argumentsJson, CreativeSession session, List<Map<String, Object>> messages) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<>() {});

            // 获取小说ID
            SessionContext context = getContext(session);
            Long novelId = args.get("novelId") != null
                ? ((Number) args.get("novelId")).longValue()
                : context.getCurrentNovelId();

            if (novelId == null) {
                return "{\"error\": \"请先创建小说\", \"errorCode\": \"NO_NOVEL\"}";
            }

            String title = (String) args.get("title");
            String content = (String) args.get("content");

            if (title == null || title.trim().isEmpty()) {
                return "{\"error\": \"章节标题不能为空\"}";
            }
            if (content == null || content.trim().isEmpty()) {
                return "{\"error\": \"章节内容不能为空\"}";
            }

            // 调用 NovelService 添加章节
            Chapter chapter = novelService.createChapter(novelId, title.trim(), content.trim(), null, null);

            // 更新上下文
            context.setCurrentChapterId(chapter.getId());
            updateContext(session, context);

            logger.info("添加章节成功: novelId={}, chapterId={}, title={}", novelId, chapter.getId(), title);

            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "chapterId", chapter.getId(),
                "title", title,
                "wordCount", content.length()
            ));
        } catch (Exception e) {
            logger.error("添加章节失败: {}", e.getMessage(), e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 创建角色卡工具实现
     */
    private String createCharacterCard(String argumentsJson, CreativeSession session, List<Map<String, Object>> messages) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<>() {});

            // 获取小说ID
            SessionContext context = getContext(session);
            Long novelId = args.get("novelId") != null
                ? ((Number) args.get("novelId")).longValue()
                : context.getCurrentNovelId();

            if (novelId == null) {
                return "{\"error\": \"请先创建小说\", \"errorCode\": \"NO_NOVEL\"}";
            }

            String name = (String) args.get("name");
            if (name == null || name.trim().isEmpty()) {
                return "{\"error\": \"角色姓名不能为空\"}";
            }

            // 生成随机 seed（1-2147483647）
            int seed = new Random().nextInt(2147483646) + 1;

            // 构建角色卡 DTO
            CharacterCard card = new CharacterCard();
            card.setName(name.trim());
            card.setAppearanceDescription((String) args.get("appearance"));
            card.setPersonality((String) args.get("personality"));
            card.setBackground((String) args.get("description"));
            // 存储 seed 在 notes 字段（临时方案）
            card.setNotes("seed:" + seed);

            // 调用 CharacterCardService 保存
            List<CharacterCard> savedCards = characterCardService.saveCharacterCards(novelId, Arrays.asList(card));
            CharacterCard savedCard = savedCards.get(0);

            // 更新上下文
            context.addCharacterId(savedCard.getId());
            updateContext(session, context);

            logger.info("创建角色卡成功: novelId={}, characterId={}, name={}, seed={}", novelId, savedCard.getId(), name, seed);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("characterId", savedCard.getId());
            result.put("name", name);
            result.put("appearance", card.getAppearanceDescription());
            result.put("personality", card.getPersonality());
            result.put("description", card.getBackground());
            result.put("role", args.get("role"));
            result.put("seed", seed);
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            logger.error("创建角色卡失败: {}", e.getMessage(), e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 更新角色卡工具实现
     */
    private String updateCharacterCard(String argumentsJson, CreativeSession session, List<Map<String, Object>> messages) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<>() {});

            Long characterId = args.get("characterId") != null
                ? ((Number) args.get("characterId")).longValue()
                : null;

            if (characterId == null) {
                // 尝试从上下文获取最近创建的角色卡
                SessionContext context = getContext(session);
                List<Long> characterIds = context.getCreatedCharacterIds();
                if (characterIds != null && !characterIds.isEmpty()) {
                    characterId = characterIds.get(characterIds.size() - 1);
                } else {
                    return "{\"error\": \"请提供角色卡ID，或先创建角色卡\", \"errorCode\": \"CHARACTER_ID_REQUIRED\"}";
                }
            }

            // 获取现有角色卡
            CharacterCard card = characterCardService.getCharacterCardById(characterId);
            if (card == null) {
                return "{\"error\": \"角色卡不存在\", \"errorCode\": \"CHARACTER_NOT_FOUND\"}";
            }

            // 更新字段（只更新提供的字段）
            if (args.get("name") != null) {
                card.setName((String) args.get("name"));
            }
            if (args.get("appearance") != null) {
                card.setAppearanceDescription((String) args.get("appearance"));
            }
            if (args.get("personality") != null) {
                card.setPersonality((String) args.get("personality"));
            }
            if (args.get("description") != null) {
                card.setBackground((String) args.get("description"));
            }

            // 保存更新
            characterCardService.updateCharacterCard(characterId, card);

            logger.info("更新角色卡成功: characterId={}, name={}", characterId, card.getName());

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("characterId", characterId);
            result.put("name", card.getName());
            result.put("appearance", card.getAppearanceDescription());
            result.put("personality", card.getPersonality());
            result.put("description", card.getBackground());
            result.put("role", args.get("role"));
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            logger.error("更新角色卡失败: {}", e.getMessage(), e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 生成角色图片工具实现
     */
    private String generateCharacterImage(String argumentsJson, CreativeSession session, SseEmitter emitter) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<>() {});

            Long characterId = ((Number) args.get("characterId")).longValue();
            String size = (String) args.getOrDefault("size", "1:1");

            // 获取角色卡
            CharacterCard card = characterCardService.getCharacterCardById(characterId);
            if (card == null) {
                return "{\"error\": \"角色卡不存在，请先创建\", \"errorCode\": \"CHARACTER_NOT_FOUND\"}";
            }

            // 构建提示词
            StringBuilder prompt = new StringBuilder();
            if (card.getAppearanceDescription() != null && !card.getAppearanceDescription().isEmpty()) {
                prompt.append(card.getAppearanceDescription());
            } else {
                prompt.append("beautiful character portrait");
            }
            prompt.append(", character art, ").append(card.getName());

            // 从 notes 中提取 seed（如果存在）
            Integer seed = null;
            if (card.getNotes() != null && card.getNotes().startsWith("seed:")) {
                try {
                    seed = Integer.parseInt(card.getNotes().substring(5).trim());
                } catch (NumberFormatException e) {
                    // 忽略解析错误
                }
            }

            // 调用 EvoLink 生成图片
            String taskId = evoLinkImageService.generateImage(prompt.toString(), size, seed);

            // 轮询获取结果（最多等待60秒）
            String imageUrl = null;
            int maxAttempts = 30;
            for (int i = 0; i < maxAttempts; i++) {
                Thread.sleep(2000);
                EvoLinkImageService.TaskStatus status = evoLinkImageService.getTaskStatus(taskId);
                if ("completed".equals(status.getStatus())) {
                    imageUrl = status.getImageUrl();
                    break;
                } else if ("failed".equals(status.getStatus())) {
                    return "{\"error\": \"图片生成失败: " + status.getError() + "\", \"errorCode\": \"IMAGE_GENERATION_FAILED\"}";
                }
            }

            if (imageUrl == null) {
                return "{\"error\": \"图片生成超时\", \"errorCode\": \"IMAGE_GENERATION_FAILED\"}";
            }

            // 更新角色卡图片
            characterCardService.updateCharacterCardAIGeneratedFields(characterId, card.getAppearanceDescription(), imageUrl);

            // 发送图片结果事件
            emitter.send(SseEmitter.event()
                .name("image_result")
                .data(objectMapper.writeValueAsString(Map.of(
                    "imageUrl", imageUrl,
                    "type", "character",
                    "id", characterId,
                    "name", card.getName()
                ))));

            logger.info("生成角色图片成功: characterId={}, imageUrl={}", characterId, imageUrl);

            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "imageUrl", imageUrl,
                "characterId", characterId,
                "characterName", card.getName()
            ));
        } catch (Exception e) {
            logger.error("生成角色图片失败: {}", e.getMessage(), e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 生成章节配图工具实现
     */
    @SuppressWarnings("unchecked")
    private String generateChapterImages(String argumentsJson, CreativeSession session, SseEmitter emitter) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<>() {});

            Long chapterId = ((Number) args.get("chapterId")).longValue();
            String mode = (String) args.getOrDefault("mode", "confirm");

            // 调用 ChapterImageService 分析章节
            List<Map<String, Object>> recommendations = chapterImageService.analyzeChapterForImages(chapterId);

            if (recommendations == null || recommendations.isEmpty()) {
                return "{\"success\": true, \"message\": \"该章节无需配图\"}";
            }

            // confirm 模式：返回推荐列表
            if ("confirm".equals(mode)) {
                // 为每个推荐构建 prompt
                List<Map<String, Object>> formattedRecs = new ArrayList<>();
                for (Map<String, Object> rec : recommendations) {
                    String description = (String) rec.get("description");
                    String prompt = chapterImageService.generateImagePrompt(description);
                    formattedRecs.add(Map.of(
                        "position", rec.get("position"),
                        "reason", rec.get("reason"),
                        "description", description,
                        "prompt", prompt
                    ));
                }

                return objectMapper.writeValueAsString(Map.of(
                    "success", true,
                    "mode", "confirm",
                    "recommendations", formattedRecs
                ));
            }

            // auto 模式：直接生成
            List<Map<String, Object>> images = new ArrayList<>();
            for (Map<String, Object> rec : recommendations) {
                String description = (String) rec.get("description");
                String prompt = chapterImageService.generateImagePrompt(description);
                String taskId = evoLinkImageService.generateImage(prompt, "16:9", null);

                // 轮询获取结果
                String imageUrl = null;
                int maxAttempts = 30;
                for (int i = 0; i < maxAttempts; i++) {
                    Thread.sleep(2000);
                    EvoLinkImageService.TaskStatus status = evoLinkImageService.getTaskStatus(taskId);
                    if ("completed".equals(status.getStatus())) {
                        imageUrl = status.getImageUrl();
                        break;
                    } else if ("failed".equals(status.getStatus())) {
                        break;
                    }
                }

                if (imageUrl != null) {
                    images.add(Map.of(
                        "imageUrl", imageUrl,
                        "position", rec.get("position")
                    ));

                    // 发送图片结果事件
                    emitter.send(SseEmitter.event()
                        .name("image_result")
                        .data(objectMapper.writeValueAsString(Map.of(
                            "imageUrl", imageUrl,
                            "type", "chapter",
                            "chapterId", chapterId,
                            "position", rec.get("position")
                        ))));
                }
            }

            logger.info("生成章节配图成功: chapterId={}, count={}", chapterId, images.size());

            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "images", images
            ));
        } catch (Exception e) {
            logger.error("生成章节配图失败: {}", e.getMessage(), e);
            return "{\"error\": \"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 生成并插入摘要（当对话过长时）
     */
    private void generateAndInsertSummary(List<Map<String, Object>> messages, SseEmitter emitter, int turnsSummarized) {
        try {
            // 保留第一条（系统提示词）和最近 N 轮对话
            Map<String, Object> systemMessage = messages.get(0);

            int messagesBefore = messages.size();

            // 构建需要摘要的内容
            List<Map<String, Object>> toSummarize = messages.subList(1, messages.size() - KEEP_RECENT_TURNS * 2);

            // 生成摘要提示
            StringBuilder summaryPrompt = new StringBuilder();
            summaryPrompt.append("请将以下对话内容生成简洁的摘要，保留关键信息：\n\n");

            for (Map<String, Object> msg : toSummarize) {
                String role = (String) msg.get("role");
                String content = (String) msg.get("content");
                if (content != null && content.length() > 200) {
                    content = content.substring(0, 200) + "...";
                }
                summaryPrompt.append(role).append(": ").append(content).append("\n");
            }

            // 调用 AI 生成摘要
            String summary = callDeepSeekForSummary(summaryPrompt.toString());

            // 替换消息历史
            messages.clear();
            messages.add(systemMessage);
            messages.add(Map.of("role", "system", "content", "[对话摘要] " + summary));

            int messagesAfter = messages.size();

            // 详细日志
            logger.info("========== 对话摘要生成 ==========");
            logger.info("摘要前消息数: {}", messagesBefore);
            logger.info("摘要后消息数: {}", messagesAfter);
            logger.info("压缩对话轮数: {}", turnsSummarized);
            logger.info("摘要内容:\n{}", summary);
            logger.info("==================================");

            // 发送 SSE 事件通知前端
            emitter.send(SseEmitter.event()
                    .name("summary_generated")
                    .data(objectMapper.writeValueAsString(Map.of(
                            "message", "对话过长，已压缩历史记录",
                            "turnsSummarized", turnsSummarized
                    ))));

        } catch (Exception e) {
            logger.error("生成摘要失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 调用 DeepSeek 生成摘要
     */
    private String callDeepSeekForSummary(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + deepSeekConfig.getApiKey());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", deepSeekConfig.getModel());
            requestBody.put("max_tokens", 500);
            requestBody.put("messages", new Object[]{
                    Map.of("role", "user", "content", prompt)
            });

            String requestBodyStr = objectMapper.writeValueAsString(requestBody);

            String response = restTemplate.execute(
                    deepSeekConfig.getApiUrl(),
                    org.springframework.http.HttpMethod.POST,
                    request -> {
                        request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                        request.getHeaders().set("Authorization", "Bearer " + deepSeekConfig.getApiKey());
                        request.getBody().write(requestBodyStr.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    },
                    responseEntity -> {
                        String body = new String(responseEntity.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        JsonNode jsonNode = objectMapper.readTree(body);
                        JsonNode choices = jsonNode.path("choices");
                        if (choices.isArray() && choices.size() > 0) {
                            return choices.get(0).path("message").path("content").asText();
                        }
                        return "摘要生成失败";
                    }
            );

            return response != null ? response : "摘要生成失败";
        } catch (Exception e) {
            logger.error("调用 DeepSeek 生成摘要失败: {}", e.getMessage());
            return "摘要生成失败";
        }
    }

    /**
     * 统计用户对话轮数
     */
    private int countUserTurns(List<Map<String, Object>> messages) {
        int count = 0;
        for (Map<String, Object> msg : messages) {
            if ("user".equals(msg.get("role"))) {
                count++;
            }
        }
        return count;
    }

    // ==================== 记忆管理 ====================

    /**
     * 获取所有记忆
     */
    public List<CreativeMemory> getAllMemories() {
        return memoryRepository.findAllByOrderByUpdateTimeDesc();
    }

    /**
     * 获取特定会话的记忆（包含全局记忆和会话记忆）
     * 会话记忆优先级高于全局记忆
     */
    public List<CreativeMemory> getMemoriesForSession(String sessionId) {
        return memoryRepository.findBySessionIdOrSessionIdIsNullOrderByUpdateTimeDesc(sessionId);
    }

    /**
     * 获取全局记忆列表
     */
    public List<CreativeMemory> getGlobalMemories() {
        return memoryRepository.findBySessionIdIsNullOrderByUpdateTimeDesc();
    }

    /**
     * 删除全局记忆
     */
    @Transactional
    public void deleteGlobalMemory(String key) {
        memoryRepository.deleteByKey(key);
        logger.info("删除全局记忆: {}", key);
    }

    /**
     * 构建包含用户记忆的系统提示词
     * @param sessionId 会话ID，用于加载会话特定记忆
     */
    private String buildSystemPromptWithMemories(String sessionId) {
        StringBuilder prompt = new StringBuilder(SYSTEM_PROMPT_BASE);

        // 获取全局记忆和会话记忆
        List<CreativeMemory> globalMemories = memoryRepository.findBySessionIdIsNullOrderByUpdateTimeDesc();
        List<CreativeMemory> sessionMemories = sessionId != null
            ? memoryRepository.findBySessionIdOrderByUpdateTimeDesc(sessionId)
            : List.of();

        // 合并记忆：会话记忆覆盖全局记忆
        Map<String, CreativeMemory> memoryMap = new LinkedHashMap<>();

        // 先添加全局记忆
        for (CreativeMemory memory : globalMemories) {
            memoryMap.put(memory.getKey(), memory);
        }

        // 再添加会话记忆（会覆盖同 key 的全局记忆）
        for (CreativeMemory memory : sessionMemories) {
            memoryMap.put(memory.getKey(), memory);
        }

        if (!memoryMap.isEmpty()) {
            prompt.append("\n\n## 用户偏好记忆\n");
            prompt.append("以下是用户之前保存的偏好，请在引导时参考：\n\n");

            for (CreativeMemory memory : memoryMap.values()) {
                String scope = memory.getSessionId() != null ? "[会话]" : "[全局]";
                prompt.append("- ").append(scope).append(" ").append(memory.getKey())
                      .append("：").append(memory.getValue()).append("\n");
            }
        }

        return prompt.toString();
    }

    /**
     * 刷新消息列表中系统消息的记忆部分
     * 每次对话请求前调用，确保记忆始终是最新的
     */
    @SuppressWarnings("unchecked")
    private void refreshMemoriesInSystemMessage(List<Map<String, Object>> messages, String sessionId) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        // 查找系统消息（通常是第一条）
        int systemMessageIndex = -1;
        for (int i = 0; i < messages.size(); i++) {
            if ("system".equals(messages.get(i).get("role"))) {
                systemMessageIndex = i;
                break;
            }
        }

        if (systemMessageIndex == -1) {
            // 没有系统消息，创建一个
            String newSystemPrompt = buildSystemPromptWithMemories(sessionId);
            messages.add(0, Map.of("role", "system", "content", newSystemPrompt));
            logger.debug("创建新的系统消息（含记忆）");
            return;
        }

        // 构建新的系统提示词
        String newSystemPrompt = buildSystemPromptWithMemories(sessionId);

        // 更新系统消息
        Map<String, Object> newSystemMessage = new LinkedHashMap<>();
        newSystemMessage.put("role", "system");
        newSystemMessage.put("content", newSystemPrompt);
        messages.set(systemMessageIndex, newSystemMessage);

        logger.debug("已刷新系统消息中的记忆部分，sessionId: {}", sessionId);
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
        tools.add(createTool("preview_params", "展示当前对话中已提取的所有创作参数", Map.of()));

        // fill_params 工具
        Map<String, Object> fillParamsProps = new LinkedHashMap<>();
        fillParamsProps.put("theme", Map.of("type", "string", "description", "题材"));
        fillParamsProps.put("style", Map.of("type", "string", "description", "风格"));
        fillParamsProps.put("protagonistName", Map.of("type", "string", "description", "主角姓名"));
        fillParamsProps.put("protagonistGender", Map.of("type", "string", "description", "主角性别"));
        fillParamsProps.put("protagonistIdentity", Map.of("type", "string", "description", "主角身份"));
        fillParamsProps.put("mainPlot", Map.of("type", "string", "description", "故事主线"));
        fillParamsProps.put("conflictType", Map.of("type", "string", "description", "核心冲突"));
        fillParamsProps.put("endingType", Map.of("type", "string", "description", "结局类型"));
        fillParamsProps.put("chapterCount", Map.of("type", "integer", "description", "章节数量"));
        fillParamsProps.put("wordsPerChapter", Map.of("type", "integer", "description", "每章字数"));
        fillParamsProps.put("pointOfView", Map.of("type", "string", "description", "叙述视角"));
        fillParamsProps.put("languageStyle", Map.of("type", "string", "description", "语言风格"));
        tools.add(createTool("fill_params", "更新创作参数。当用户明确选择或确认某个参数值时调用。", fillParamsProps));

        // add_memory 工具
        Map<String, Object> addMemoryProps = new LinkedHashMap<>();
        addMemoryProps.put("key", Map.of("type", "string", "description", "记忆类型，如 preferred_style, preferred_genre"));
        addMemoryProps.put("value", Map.of("type", "string", "description", "记忆内容"));
        addMemoryProps.put("scope", Map.of("type", "string", "description", "记忆范围：global（全局，跨会话共享）或 session（仅当前会话）", "enum", Arrays.asList("global", "session")));
        tools.add(createTool("add_memory", "添加记忆。仅当用户明确要求「记住」「保存偏好」时调用。默认保存为全局记忆，用户指定「本次会话」时保存为会话记忆。", addMemoryProps, Arrays.asList("key", "value")));

        // remove_memory 工具
        Map<String, Object> removeMemoryProps = new LinkedHashMap<>();
        removeMemoryProps.put("key", Map.of("type", "string", "description", "要删除的记忆类型"));
        removeMemoryProps.put("scope", Map.of("type", "string", "description", "记忆范围：global（全局）或 session（会话）", "enum", Arrays.asList("global", "session")));
        tools.add(createTool("remove_memory", "删除记忆。当用户要求「忘记」「删除偏好」时调用。默认删除全局记忆。", removeMemoryProps, Arrays.asList("key")));

        // show_examples 工具
        Map<String, Object> showExamplesProps = new LinkedHashMap<>();
        showExamplesProps.put("genre", Map.of("type", "string", "description", "题材类型"));
        tools.add(createTool("show_examples", "展示相似题材的作品片段作为参考", showExamplesProps, Arrays.asList("genre")));

        // ========== 新增创作工具 ==========

        // create_novel 工具
        Map<String, Object> createNovelProps = new LinkedHashMap<>();
        createNovelProps.put("title", Map.of("type", "string", "description", "小说标题"));
        tools.add(createTool("create_novel", "创建一部新小说。当用户明确提供小说标题时调用。", createNovelProps, Arrays.asList("title")));

        // add_chapter 工具
        Map<String, Object> addChapterProps = new LinkedHashMap<>();
        addChapterProps.put("novelId", Map.of("type", "integer", "description", "小说ID，不填则使用当前会话的小说"));
        addChapterProps.put("title", Map.of("type", "string", "description", "章节标题"));
        addChapterProps.put("content", Map.of("type", "string", "description", "章节内容"));
        addChapterProps.put("afterChapterId", Map.of("type", "integer", "description", "插入到指定章节之后"));
        tools.add(createTool("add_chapter", "为小说添加新章节。需要先创建小说。", addChapterProps, Arrays.asList("title", "content")));

        // create_character_card 工具
        Map<String, Object> createCharProps = new LinkedHashMap<>();
        createCharProps.put("novelId", Map.of("type", "integer", "description", "小说ID"));
        createCharProps.put("name", Map.of("type", "string", "description", "角色姓名"));
        createCharProps.put("role", Map.of("type", "string", "description", "角色定位：主角/配角/反派/路人"));
        createCharProps.put("description", Map.of("type", "string", "description", "角色描述"));
        createCharProps.put("appearance", Map.of("type", "string", "description", "外貌特征（用于生成图片）"));
        createCharProps.put("personality", Map.of("type", "string", "description", "性格特点"));
        tools.add(createTool("create_character_card", "创建角色卡。自动生成seed用于图片一致性。当讨论角色细节时主动创建。", createCharProps, Arrays.asList("name")));

        // update_character_card 工具
        Map<String, Object> updateCharProps = new LinkedHashMap<>();
        updateCharProps.put("characterId", Map.of("type", "integer", "description", "角色卡ID"));
        updateCharProps.put("name", Map.of("type", "string", "description", "角色姓名"));
        updateCharProps.put("role", Map.of("type", "string", "description", "角色定位：主角/配角/反派/路人"));
        updateCharProps.put("description", Map.of("type", "string", "description", "角色描述"));
        updateCharProps.put("appearance", Map.of("type", "string", "description", "外貌特征（用于生成图片）"));
        updateCharProps.put("personality", Map.of("type", "string", "description", "性格特点"));
        tools.add(createTool("update_character_card", "更新已存在的角色卡信息。当用户补充或修改角色设定时调用。", updateCharProps, Arrays.asList("characterId")));

        // generate_character_image 工具
        Map<String, Object> genCharImgProps = new LinkedHashMap<>();
        genCharImgProps.put("characterId", Map.of("type", "integer", "description", "角色卡ID"));
        genCharImgProps.put("size", Map.of("type", "string", "description", "图片尺寸", "enum", Arrays.asList("1:1", "16:9", "9:16")));
        tools.add(createTool("generate_character_image", "根据角色卡生成角色图片，使用seed保持一致性。", genCharImgProps, Arrays.asList("characterId")));

        // generate_chapter_images 工具
        Map<String, Object> genChapterImgProps = new LinkedHashMap<>();
        genChapterImgProps.put("chapterId", Map.of("type", "integer", "description", "章节ID"));
        genChapterImgProps.put("mode", Map.of("type", "string", "description", "auto-自动生成/confirm-先确认后生成", "enum", Arrays.asList("auto", "confirm")));
        tools.add(createTool("generate_chapter_images", "为章节生成配图。confirm模式返回推荐位置供用户确认。", genChapterImgProps, Arrays.asList("chapterId")));

        return tools;
    }

    /**
     * 创建工具定义
     */
    private Map<String, Object> createTool(String name, String description, Map<String, Object> properties) {
        return createTool(name, description, properties, Collections.emptyList());
    }

    private Map<String, Object> createTool(String name, String description, Map<String, Object> properties, List<String> required) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("type", "object");
        params.put("properties", properties);
        params.put("required", required);
        params.put("additionalProperties", false);

        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", name);
        function.put("strict", true);
        function.put("description", description);
        function.put("parameters", params);

        return Map.of("type", "function", "function", function);
    }

    // ==================== 系统提示词 ====================

    /**
     * 构建系统提示词基础部分（固定不变，利于缓存）
     */
    private static String buildSystemPromptBase() {
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

            ## 参数更新规则

            **重要**：参数更新由你主动调用 fill_params 工具完成。

            ### 何时更新参数
            - 用户明确选择了某个选项 → 调用 fill_params 更新对应参数
            - 用户描述了清晰的想法 → 确认理解后调用 fill_params 更新
            - 用户输入自定义内容 → 理解并确认后调用 fill_params 更新

            ### 何时不更新参数
            - 用户选择"其他"或"自定义" → 追问用户具体想要什么
            - 用户表达不确定 → 继续引导，不更新
            - 用户只是闲聊 → 不更新

            ### 追问示例
            用户选择"其他风格"时：
            "好的，那你能描述一下你想要的风格吗？比如是偏向治愈系、还是带点小虐心的？"

            ### 确认示例
            用户描述后：
            "让我确认一下：**风格** = 治愈系。这样理解对吗？"
            用户确认后，调用 fill_params 更新参数。

            ## 记忆管理

            支持两种记忆范围：
            - **全局记忆**（scope=global）：跨会话共享，适合用户长期偏好
            - **会话记忆**（scope=session）：仅当前会话有效，适合特定故事的设定

            调用时机：
            - 用户说"记住我喜欢的风格" → add_memory(scope=global)
            - 用户说"这个设定只用于本次故事" → add_memory(scope=session)
            - 用户说"忘记之前的偏好" → remove_memory

            默认使用全局记忆，除非用户明确指定"本次会话"。

            ## 回复格式

            使用 Markdown 格式回复。**每个问题都会显示选项 + 自定义输入框**，用户可以点击选项，也可以直接输入。

            ### 选项标记格式（必须使用）

            格式：`[OPTIONS:字段名:显示名称]选项1|选项2|选项3[/OPTIONS]`

            **单个问题：**
            ```
            你想写什么题材？
            [OPTIONS:theme:题材]古代宫廷|现代都市|玄幻修仙|科幻未来|悬疑推理[/OPTIONS]
            ```

            **多个问题（一次最多2-3个）：**
            ```
            让我们确定几个关键设定：

            [OPTIONS:theme:题材]古代宫廷|现代都市|玄幻修仙[/OPTIONS]

            [OPTIONS:style:风格]甜蜜温馨|虐心催泪|轻松搞笑|暗黑复仇[/OPTIONS]

            [OPTIONS:protagonistGender:主角性别]男|女|双主角[/OPTIONS]
            ```

            **开放式问题（如姓名）：也提供参考选项**
            ```
            她叫什么名字呢？

            [OPTIONS:protagonistName:主角姓名]林清雅|苏念柔|沈晚晴|江映雪[/OPTIONS]
            ```

            ### 格式说明
            - `[OPTIONS:字段名:显示名称]选项1|选项2|选项3[/OPTIONS]`
            - **字段名**：英文标识符，用于后端存储（如 theme, protagonistName）
            - **显示名称**：中文显示名，用于前端展示（如 题材, 主角姓名）
            - 选项用 `|` 分隔
            - 每个问题下方**自动显示输入框**，用户可直接输入自定义内容

            ### 其他格式要求
            - **加粗** 强调关键词
            - 确认信息不需要选项标记

            ## 字段名对照表

            | 字段名 | 含义 | 常见选项 |
            |--------|------|----------|
            | theme | 题材 | 古代宫廷、现代都市、玄幻修仙、科幻未来、悬疑推理、民国谍战、奇幻冒险、校园爱情 |
            | style | 风格 | 甜蜜温馨、暗恋成真、欢喜冤家、虐心催泪、轻松搞笑、暗黑复仇、治愈系 |
            | protagonistGender | 主角性别 | 男、女、双主角 |
            | protagonistName | 主角姓名 | 开放式，可提供参考选项 |
            | protagonistIdentity | 主角身份 | 皇帝/公主、总裁/秘书、修仙者、学生、教师、医生/律师等 |
            | protagonistAge | 主角年龄 | 开放式，可提供参考：十八九岁、二十出头、二十五六、三十左右 |
            | mainPlot | 故事主线 | 开放式，引导用户描述 |
            | conflictType | 核心冲突 | 身份对立、家族恩怨、误会隔阂、命运捉弄、情敌竞争 |
            | endingType | 结局类型 | 圆满结局、悲剧结局、开放式结局、复仇成功、逆袭成功 |
            | chapterCount | 章节数量 | 5-10章|10-20章|20-50章|50章以上 |
            | wordsPerChapter | 每章字数 | 2000字|3000字|5000字 |
            | languageStyle | 语言风格 | 文艺唯美、幽默诙谐、直白简洁、诗意抒情 |
            | pointOfView | 叙述视角 | 第三人称全知、第一人称、第三人称有限视角 |

            ## 提问格式规范（必须遵守）

            **重要**：每个问题必须紧跟其选项，不允许问题和答案分离。

            ✅ 正确：问题 → 选项 → 问题 → 选项
            ❌ 错误：问题1 → 问题2 → 选项1 → 选项2

            示例：
            你想写什么题材？
            [OPTIONS:theme:题材]古代宫廷|现代都市|玄幻修仙|科幻未来[/OPTIONS]

            你希望是什么风格？
            [OPTIONS:style:风格]甜蜜温馨|虐心催泪|轻松搞笑|暗黑复仇[/OPTIONS]

            ## 多选说明

            - 用户可以在同一字段选择多个选项
            - 多选值以数组格式存储
            - 例如：题材可以同时选择「现代都市」和「校园爱情」

            支持多选的字段：theme（题材）、style（风格）、conflictType（冲突类型）
            单选字段：protagonistGender、protagonistName、endingType

            ## 创作工具

            当用户明确要求创建小说、添加章节、创建角色卡或生成图片时，调用相应的工具：

            - create_novel：创建小说（需要先询问标题）
            - add_chapter：添加章节（需要小说已创建）
            - create_character_card：创建角色卡（需要小说已创建）
            - update_character_card：更新角色卡信息
            - generate_character_image：生成角色图片（需要角色卡已创建）
            - generate_chapter_images：生成章节配图（需要章节已创建）

            ## 角色卡主动创建规则

            **重要**：当讨论角色细节时，你应该主动创建或更新角色卡，而不是等待用户明确要求。

            ### 何时主动创建角色卡
            - 用户详细描述了主角或重要配角的外貌、性格、背景
            - 用户提到"这个角色..."并给出具体设定
            - 用户补充了角色的更多信息

            ### 创建时机
            1. 当用户首次完整描述一个角色时 → 调用 create_character_card
            2. 当用户补充或修改已有角色信息时 → 调用 update_character_card

            ### 示例场景
            - 用户说"主角叫林清雅，是个清冷型的美少女" → 创建角色卡
            - 用户说"她有一头银色长发" → 更新角色卡（添加外貌描述）
            - 用户说"性格有点傲娇" → 更新角色卡（添加性格）

            ### 注意事项
            - 创建角色卡前，小说必须已存在
            - 每个角色创建后返回 characterId，后续更新时使用该ID
            - 如果用户没有明确说小说标题，先创建小说或询问
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

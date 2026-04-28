package org.zenithon.articlecollect.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.zenithon.articlecollect.dto.DeepSeekConfigDTO;
import org.zenithon.articlecollect.dto.DeepSeekRuntimeConfig;
import org.zenithon.articlecollect.service.AIPromptService;
import org.zenithon.articlecollect.service.DeepSeekConfigService;
import org.zenithon.articlecollect.entity.DeepSeekFeatureConfig.FeatureCode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI 对话控制器
 */
@RestController
@RequestMapping("/api/ai")
public class AIChatController {

    private static final Logger logger = LoggerFactory.getLogger(AIChatController.class);

    private final AIPromptService aiPromptService;
    private final DeepSeekConfigService deepSeekConfigService;

    public AIChatController(AIPromptService aiPromptService, DeepSeekConfigService deepSeekConfigService) {
        this.aiPromptService = aiPromptService;
        this.deepSeekConfigService = deepSeekConfigService;
    }

    /**
     * 流式 AI 对话接口 - POST 方式
     * 支持可选的 config 参数覆盖默认配置
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStreamPost(@RequestBody(required = false) Map<String, Object> request) {
        String prompt = request != null ? (String) request.get("prompt") : null;

        // 提取可选的运行时配置
        DeepSeekRuntimeConfig config = null;
        if (request != null && request.containsKey("config")) {
            config = extractConfig(request.get("config"));
        }

        logger.info("收到用户请求: {}, hasConfig: {}", prompt, config != null);
        return handleChatStream(prompt, config);
    }

    /**
     * 普通 AI 对话接口 - POST 方式（非流式）
     * 支持可选的 config 参数覆盖默认配置
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chatPost(@RequestBody(required = false) Map<String, Object> request) {
        String prompt = request != null ? (String) request.get("prompt") : null;
        logger.info("收到用户请求：" + prompt);

        if (prompt == null || prompt.trim().isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "提示词不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        // 提取可选的运行时配置
        DeepSeekRuntimeConfig config = null;
        if (request != null && request.containsKey("config")) {
            config = extractConfig(request.get("config"));
        }

        try {
            // 同步调用 AI 服务
            String aiResponse = callAISync(prompt, config);

            Map<String, String> response = new HashMap<>();
            response.put("response", aiResponse);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("AI 对话失败：" + e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "AI 对话失败：" + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 从请求中提取配置
     */
    @SuppressWarnings("unchecked")
    private DeepSeekRuntimeConfig extractConfig(Object configObj) {
        if (configObj instanceof Map) {
            Map<String, Object> configMap = (Map<String, Object>) configObj;
            DeepSeekRuntimeConfig config = new DeepSeekRuntimeConfig();
            if (configMap.containsKey("model")) {
                config.setModel((String) configMap.get("model"));
            }
            if (configMap.containsKey("thinkingEnabled")) {
                config.setThinkingEnabled((Boolean) configMap.get("thinkingEnabled"));
            }
            if (configMap.containsKey("reasoningEffort")) {
                config.setReasoningEffort((String) configMap.get("reasoningEffort"));
            }
            return config;
        }
        return null;
    }

    /**
     * 同步调用 AI 服务获取响应
     */
    private String callAISync(String prompt, DeepSeekRuntimeConfig config) throws Exception {
        return aiPromptService.callDeepSeekAPIWithConfig(prompt, config);
    }

    /**
     * 流式 AI 对话接口 - GET 方式 (用于 EventSource)
     */
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStreamGet(@RequestParam("prompt") String prompt) {
        return handleChatStream(prompt, null);
    }

    /**
     * 处理聊天请求的公共方法
     */
    private SseEmitter handleChatStream(String prompt, DeepSeekRuntimeConfig config) {
        // 校验提示词是否为空
        if (prompt == null || prompt.trim().isEmpty()) {
            // 创建立即返回的 Emitter
            SseEmitter emitter = new SseEmitter();
            try {
                // 发送错误事件到前端
                emitter.send(SseEmitter.event()
                    .name("error")  // 事件名称
                    .data("{\"error\": \"提示词不能为空\"}"));  // 错误信息
            } catch (IOException e) {
                logger.error("发送错误消息失败", e);
            }
            return emitter;
        }

        // 如果没有配置，使用默认配置
        final DeepSeekRuntimeConfig finalConfig;
        if (config == null) {
            finalConfig = deepSeekConfigService.getDefaultRuntimeConfig(FeatureCode.AI_CHAT);
        } else {
            finalConfig = config;
        }

        // 创建 SSE 发射器，设置超时时间为 5 分钟
        // SseEmitter 用于保持与服务器的长连接，实现流式数据传输
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);

        CompletableFuture.runAsync(() -> {
            try {
                aiPromptService.chatStreamWithConfig(prompt, emitter, finalConfig);
                emitter.complete();
            } catch (Exception e) {
                logger.error("AI 对话失败：" + e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"error\": \"" + escapeJson(e.getMessage()) + "\"}"));
                    emitter.completeWithError(e);
                } catch (IOException ex) {
                    logger.error("发送错误消息失败", ex);
                }
            }
        });

        emitter.onCompletion(() -> logger.info("SSE 连接正常关闭"));
        emitter.onTimeout(() -> {
            logger.warn("SSE 连接超时");
            emitter.completeWithError(new RuntimeException("请求超时"));
        });
        emitter.onError((throwable) -> logger.error("SSE 连接错误：" + throwable.getMessage(), throwable));

        return emitter;
    }
    
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}

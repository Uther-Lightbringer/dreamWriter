package org.zenithon.articlecollect.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.zenithon.articlecollect.service.AIPromptService;

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
    
    public AIChatController(AIPromptService aiPromptService) {
        this.aiPromptService = aiPromptService;
    }
    
    /**
     * 流式 AI 对话接口 - POST 方式
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStreamPost(@RequestBody(required = false) Map<String, String> request) {
        String prompt = request != null ? request.get("prompt") : null;
        logger.info("收到用户请求：" + prompt);
        return handleChatStream(prompt);
    }
    
    /**
     * 普通 AI 对话接口 - POST 方式（非流式）
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chatPost(@RequestBody(required = false) Map<String, String> request) {
        String prompt = request != null ? request.get("prompt") : null;
        logger.info("收到用户请求：" + prompt);
        
        if (prompt == null || prompt.trim().isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "提示词不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            // 同步调用 AI 服务
            String aiResponse = callAISync(prompt);
            
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
     * 同步调用 AI 服务获取响应
     */
    private String callAISync(String prompt) throws Exception {
        // 直接调用 AIPromptService 中的 DeepSeek API 方法
        // 注：更好的方式是在 AIPromptService 中添加一个同步方法
        return aiPromptService.callDeepSeekAPI(prompt);
    }
    
    /**
     * 流式 AI 对话接口 - GET 方式 (用于 EventSource)
     */
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStreamGet(@RequestParam String prompt) {
        return handleChatStream(prompt);
    }
    
    /**
     * 处理聊天请求的公共方法
     */
    private SseEmitter handleChatStream(String prompt) {
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
        
        // 创建 SSE 发射器，设置超时时间为 5 分钟
        // SseEmitter 用于保持与服务器的长连接，实现流式数据传输
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
        
        CompletableFuture.runAsync(() -> {
            try {
                aiPromptService.chatStream(prompt, emitter);
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

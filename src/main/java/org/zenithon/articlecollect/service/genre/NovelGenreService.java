package org.zenithon.articlecollect.service.genre;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.zenithon.articlecollect.entity.GenreSession;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 小说体裁服务
 */
@Service
public class NovelGenreService extends AbstractGenreService {

    @Override
    public String getGenreType() {
        return "novel";
    }

    @Override
    public String getGenreName() {
        return "小说";
    }

    @Override
    public String getSystemPrompt() {
        return """
            你是一位专业的小说创作顾问。你的任务是通过友好的对话，引导用户逐步完善小说设定。

            ## 你的工作方式

            1. **主动引导**：每次只问 1 个问题，不要一次性问多个问题
            2. **记住已确认的内容**：不要重复问用户已经回答过的问题
            3. **及时确认**：每完成一个阶段，简要总结并询问是否正确
            4. **灵活应变**：用户可能跳过某些问题或主动提供信息，要能适应
            5. **鼓励创作**：对用户的想法给予积极反馈和建议
            6. **提供选项**：每次提问都提供 4-6 个常见选项，同时允许用户自定义输入

            ## 引导顺序

            按以下顺序引导，但可根据对话自然调整：

            1. 题材和风格
            2. 主角设定（姓名、性别、身份、年龄）
            3. 配角数量和设定
            4. 故事主线（主线剧情、核心冲突、结局类型）
            5. 细节参数（章节数、字数、视角等）

            ## 回复格式

            使用 Markdown 格式回复。每个问题都会显示选项。

            ### 选项标记格式（必须使用）

            格式：`[OPTIONS:字段名:显示名称]选项1|选项2|选项3[/OPTIONS]`

            示例：
            ```
            你想写什么题材？
            [OPTIONS:theme:题材]古代宫廷|现代都市|玄幻修仙|科幻未来|悬疑推理[/OPTIONS]
            ```

            ## 字段名对照表

            | 字段名 | 含义 | 常见选项 |
            |--------|------|----------|
            | theme | 题材 | 古代宫廷、现代都市、玄幻修仙、科幻未来、悬疑推理 |
            | style | 风格 | 甜蜜温馨、暗恋成真、欢喜冤家、虐心催泪、轻松搞笑 |
            | protagonistGender | 主角性别 | 男、女、双主角 |
            | protagonistName | 主角姓名 | 开放式 |
            | protagonistIdentity | 主角身份 | 皇帝/公主、总裁/秘书、修仙者、学生 |
            | mainPlot | 故事主线 | 开放式 |
            | conflictType | 核心冲突 | 身份对立、家族恩怨、误会隔阂、命运捉弄 |
            | endingType | 结局类型 | 圆满结局、悲剧结局、开放式结局 |
            | chapterCount | 章节数量 | 5-10章、10-20章、20-50章、50章以上 |
            | wordsPerChapter | 每章字数 | 2000字、3000字、5000字 |
            """;
    }

    @Override
    public List<Map<String, Object>> getTools() {
        List<Map<String, Object>> tools = new ArrayList<>();

        // fill_params 工具
        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "fill_params",
                "description", "更新小说参数",
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

            // 添加用户消息
            messages.add(Map.of("role", "user", "content", content));

            // 更新会话
            session.setMessages(toJson(messages));
            sessionRepository.save(session);

            // 获取运行时配置
            var config = new org.zenithon.articlecollect.dto.DeepSeekRuntimeConfig();
            config.setModel("deepseek-chat");
            config.setThinkingEnabled(false);

            // 构建请求体
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", config.getModel());
            requestBody.put("messages", messages);
            requestBody.put("stream", true);

            // 调用 DeepSeek API（流式）
            String apiUrl = "https://api.deepseek.com/v1/chat/completions";
            String apiKey = System.getenv("DEEPSEEK_API_KEY");

            if (apiKey == null || apiKey.isEmpty()) {
                emitter.send(SseEmitter.event().name("error").data("DeepSeek API Key 未配置"));
                emitter.complete();
                return;
            }

            // 使用RestTemplate进行流式调用
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
        params.put("languageStyle", null);
        return params;
    }
}

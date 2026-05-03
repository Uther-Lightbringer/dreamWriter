package org.zenithon.articlecollect.service.genre;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.zenithon.articlecollect.entity.GenreSession;
import org.zenithon.articlecollect.entity.VisualWork;
import org.zenithon.articlecollect.entity.VisualPanel;
import org.zenithon.articlecollect.entity.CharacterCardEntity;
import org.zenithon.articlecollect.repository.VisualWorkRepository;
import org.zenithon.articlecollect.repository.VisualPanelRepository;
import org.zenithon.articlecollect.repository.CharacterCardRepository;
import org.zenithon.articlecollect.service.EvoLinkImageService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 视觉叙事体裁服务
 */
@Service
public class VisualGenreService extends AbstractGenreService {

    @Autowired
    private VisualWorkRepository visualWorkRepository;

    @Autowired
    private VisualPanelRepository visualPanelRepository;

    @Autowired
    private CharacterCardRepository characterCardRepository;

    @Autowired
    private EvoLinkImageService evoLinkImageService;

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
            3. **角色设定**：主角、配角的外貌和性格（必须创建角色卡！）
            4. **场景设定**：主要场景和环境描述
            5. **画面风格**：写实、卡通、水彩、像素等
            6. **分镜设计**：镜头语言、构图要求

            ## 角色设定流程（重要）

            **关键要求**：每个角色在确定基本信息后，必须创建角色卡！

            ### 角色卡必填信息
            - 姓名、性别、年龄
            - **完整外貌描述**（用于AI绘图一致性）：
              - 身高（如：168cm）
              - 发型发色（如：齐腰黑发，微卷）
              - 眼睛描述（如：丹凤眼，深褐色瞳孔）
              - 脸型（如：瓜子脸）
              - 体型（如：身材苗条）
              - 服装风格（如：白色衬衫配淡蓝色长裙）
              - 显著特征（如：左眉有一道疤痕）

            ### 角色设定示例
            用户确认角色信息后，立即调用 `create_character_card` 工具创建角色卡：
            ```
            好的，我已经记录下沈清漪的基本信息：
            - 性别：女
            - 年龄：26岁
            - 身份：富豪千金

            现在让我为她创建角色卡，记录完整的外貌信息...
            ```

            ## 分镜描述规则（关键）

            **重要**：分镜描述中必须包含角色的完整外貌信息，确保AI绘图一致性！

            ### 规则
            1. 每个角色**首次出场**时，必须附带完整外貌描述
            2. 后续出场可简化，但仍需包含关键特征（性别、发色、服装）
            3. 外貌描述必须具体，避免比喻

            ### 正确示例
            ```
            分镜1 [中景]：
            夜晚，奢华别墅主卧。沈清漪（女，26岁，齐腰黑发微卷，瓜子脸，身材苗条，身穿墨绿色晚礼裙）坐在梳妆台前，神情疲倦。林小鹿（女，22岁，齐肩棕色短发，圆脸，身穿整洁的黑色女仆制服）站在她身后，正在帮她解开项链。
            ```

            ### 错误示例
            ```
            分镜1 [中景]：
            夜晚，奢华别墅主卧。沈清漪坐在梳妆台前，神情疲倦。林小鹿站在她身后。
            ```
            ❌ 缺少角色外貌描述，会导致AI绘图时角色外观不一致！

            ## 分镜描述格式

            ```
            分镜X [镜头角度]：
            [包含角色完整外貌的画面描述]。[动作描述]。[环境/光影描述]。
            对话：[对话内容]
            音效：[音效描述]
            ```

            ## 可用工具

            - `create_character_card`：创建角色卡（角色确定后立即调用）
            - `list_character_cards`：查看已有角色卡
            - `update_character_card`：更新角色卡信息
            - `create_visual_work`：创建视觉叙事作品
            - `add_panel`：添加分镜
            - `update_panel`：修改分镜
            - `get_panel_summaries`：获取分镜概括
            - `generate_storyboard`：生成分镜脚本
            - `generate_panel_image`：为分镜生成图片
            - `generate_reference_image`：生成参考图片

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

        // fill_params
        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "fill_params",
                "description", "更新视觉叙事参数",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "params", Map.of("type", "object", "description", "要更新的参数键值对")
                    ),
                    "required", List.of("params")
                )
            )
        ));

        // list_character_cards - 查看角色卡
        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "list_character_cards",
                "description", "查看已有角色卡列表。创建新角色前必须先调用此工具查看现有角色，避免重复创建。",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(),
                    "required", List.of()
                )
            )
        ));

        // create_character_card - 创建角色卡
        Map<String, Object> appearanceProps = new LinkedHashMap<>();
        appearanceProps.put("height", Map.of("type", "string", "description", "身高，如：168cm"));
        appearanceProps.put("hair", Map.of("type", "string", "description", "发型发色，如：齐腰黑发，微卷"));
        appearanceProps.put("eyes", Map.of("type", "string", "description", "眼睛描述，如：丹凤眼，深褐色瞳孔"));
        appearanceProps.put("face", Map.of("type", "string", "description", "脸型，如：瓜子脸"));
        appearanceProps.put("build", Map.of("type", "string", "description", "体型，如：身材苗条"));
        appearanceProps.put("clothing", Map.of("type", "string", "description", "服装，如：白色衬衫配淡蓝色长裙"));
        appearanceProps.put("legwear", Map.of("type", "string", "description", "腿部穿着"));
        appearanceProps.put("shoes", Map.of("type", "string", "description", "鞋子"));
        appearanceProps.put("accessories", Map.of("type", "string", "description", "配饰"));
        appearanceProps.put("distinguishingFeatures", Map.of("type", "string", "description", "显著特征"));
        Map<String, Object> appearanceObj = new LinkedHashMap<>();
        appearanceObj.put("type", "object");
        appearanceObj.put("description", "外貌特征（必须填写所有必填字段，用于AI生成角色图片）");
        appearanceObj.put("properties", appearanceProps);

        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "create_character_card",
                "description", "创建角色卡。角色信息确认后立即调用。必须填写完整外貌信息用于AI绘图一致性。每部作品只能有一个主角。",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.ofEntries(
                        Map.entry("name", Map.of("type", "string", "description", "角色姓名")),
                        Map.entry("role", Map.of("type", "string", "description", "角色定位：protagonist(主角)/supporting(配角)/antagonist(反派)", "enum", List.of("protagonist", "supporting", "antagonist"))),
                        Map.entry("gender", Map.of("type", "string", "description", "性别")),
                        Map.entry("age", Map.of("type", "integer", "description", "年龄")),
                        Map.entry("occupation", Map.of("type", "string", "description", "职业/身份")),
                        Map.entry("appearance", appearanceObj),
                        Map.entry("personality", Map.of("type", "string", "description", "性格特点")),
                        Map.entry("description", Map.of("type", "string", "description", "角色背景描述"))
                    ),
                    "required", List.of("name", "gender", "appearance")
                )
            )
        ));

        // update_character_card - 更新角色卡
        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "update_character_card",
                "description", "更新已有角色卡信息。用户补充或修改角色设定时调用。",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.ofEntries(
                        Map.entry("characterId", Map.of("type", "integer", "description", "角色卡ID")),
                        Map.entry("name", Map.of("type", "string", "description", "角色姓名")),
                        Map.entry("role", Map.of("type", "string", "description", "角色定位", "enum", List.of("protagonist", "supporting", "antagonist"))),
                        Map.entry("gender", Map.of("type", "string", "description", "性别")),
                        Map.entry("age", Map.of("type", "integer", "description", "年龄")),
                        Map.entry("occupation", Map.of("type", "string", "description", "职业/身份")),
                        Map.entry("appearance", appearanceObj),
                        Map.entry("personality", Map.of("type", "string", "description", "性格特点")),
                        Map.entry("description", Map.of("type", "string", "description", "角色背景描述"))
                    ),
                    "required", List.of("characterId")
                )
            )
        ));

        // create_visual_work
        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "create_visual_work",
                "description", "创建视觉叙事作品",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "title", Map.of("type", "string", "description", "作品标题"),
                        "visualType", Map.of("type", "string", "description", "作品类型：漫画脚本、分镜脚本、故事板、绘本脚本"),
                        "description", Map.of("type", "string", "description", "作品简介")
                    ),
                    "required", List.of("title", "visualType")
                )
            )
        ));

        // add_panel
        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "add_panel",
                "description", "添加分镜。画面描述必须包含角色的完整外貌信息（性别、发型、服装等）。",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "workId", Map.of("type", "integer", "description", "作品ID"),
                        "panelNumber", Map.of("type", "integer", "description", "分镜序号"),
                        "scene", Map.of("type", "string", "description", "画面描述（必须包含角色外貌信息）"),
                        "cameraAngle", Map.of("type", "string", "description", "镜头角度：特写、中景、远景、俯视、仰视"),
                        "dialogue", Map.of("type", "string", "description", "对话内容"),
                        "soundEffect", Map.of("type", "string", "description", "音效描述"),
                        "action", Map.of("type", "string", "description", "动作描述"),
                        "summary", Map.of("type", "string", "description", "分镜概括")
                    ),
                    "required", List.of("workId", "panelNumber", "scene")
                )
            )
        ));

        // update_panel
        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "update_panel",
                "description", "修改分镜",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "panelId", Map.of("type", "integer", "description", "分镜ID"),
                        "scene", Map.of("type", "string", "description", "画面描述"),
                        "dialogue", Map.of("type", "string", "description", "对话内容"),
                        "summary", Map.of("type", "string", "description", "分镜概括")
                    ),
                    "required", List.of("panelId", "scene")
                )
            )
        ));

        // get_panel_summaries
        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "get_panel_summaries",
                "description", "获取作品所有分镜概括",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "workId", Map.of("type", "integer", "description", "作品ID")
                    ),
                    "required", List.of("workId")
                )
            )
        ));

        // generate_storyboard
        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "generate_storyboard",
                "description", "生成分镜脚本",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "workId", Map.of("type", "integer", "description", "作品ID"),
                        "storyboard", Map.of("type", "string", "description", "分镜脚本内容")
                    ),
                    "required", List.of("workId", "storyboard")
                )
            )
        ));

        // update_visual_work
        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "update_visual_work",
                "description", "修改视觉叙事作品信息",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "workId", Map.of("type", "integer", "description", "作品ID"),
                        "title", Map.of("type", "string", "description", "作品标题"),
                        "description", Map.of("type", "string", "description", "作品简介"),
                        "visualType", Map.of("type", "string", "description", "作品类型")
                    ),
                    "required", List.of("workId")
                )
            )
        ));

        // update_storyboard
        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "update_storyboard",
                "description", "更新分镜脚本",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "workId", Map.of("type", "integer", "description", "作品ID"),
                        "storyboard", Map.of("type", "string", "description", "分镜脚本内容")
                    ),
                    "required", List.of("workId", "storyboard")
                )
            )
        ));

        // generate_panel_image
        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "generate_panel_image",
                "description", "根据分镜的场景描述生成图片。会自动读取分镜的画面描述、镜头角度等信息来构建提示词。",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "panelId", Map.of("type", "integer", "description", "分镜ID"),
                        "size", Map.of("type", "string", "description", "图片尺寸", "enum", List.of("1:1", "16:9", "9:16"))
                    ),
                    "required", List.of("panelId")
                )
            )
        ));

        // generate_reference_image
        tools.add(Map.of(
            "type", "function",
            "function", Map.of(
                "name", "generate_reference_image",
                "description", "生成角色、场景或道具的参考图片。当用户需要视觉参考时调用。",
                "parameters", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "prompt", Map.of("type", "string", "description", "图片描述提示词，详细描述要生成的内容"),
                        "size", Map.of("type", "string", "description", "推荐尺寸：1:1角色图、16:9场景图、9:16竖版图", "enum", List.of("1:1", "16:9", "9:16")),
                        "type", Map.of("type", "string", "description", "图片类型：角色/场景/道具/其他", "enum", List.of("character", "scene", "prop", "other"))
                    ),
                    "required", List.of("prompt")
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
                    case "list_character_cards":
                        result = executeListCharacterCards();
                        break;
                    case "create_character_card":
                        result = executeCreateCharacterCard(argumentsJson);
                        break;
                    case "update_character_card":
                        result = executeUpdateCharacterCard(argumentsJson);
                        break;
                    case "create_visual_work":
                        result = executeCreateVisualWork(argumentsJson, session);
                        break;
                    case "add_panel":
                        result = executeAddPanel(argumentsJson);
                        break;
                    case "update_panel":
                        result = executeUpdatePanel(argumentsJson);
                        break;
                    case "get_panel_summaries":
                        result = executeGetPanelSummaries(argumentsJson);
                        break;
                    case "generate_storyboard":
                        result = executeGenerateStoryboard(argumentsJson);
                        break;
                    case "update_visual_work":
                        result = executeUpdateVisualWork(argumentsJson);
                        break;
                    case "update_storyboard":
                        result = executeUpdateStoryboard(argumentsJson);
                        break;
                    case "generate_panel_image":
                        result = executeGeneratePanelImage(argumentsJson, emitter);
                        break;
                    case "generate_reference_image":
                        result = executeGenerateReferenceImage(argumentsJson, emitter);
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

    // ==================== 角色卡工具实现 ====================

    private String executeListCharacterCards() {
        try {
            List<CharacterCardEntity> cards = characterCardRepository.findAll();
            List<Map<String, Object>> result = new ArrayList<>();
            for (CharacterCardEntity card : cards) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", card.getId());
                item.put("name", card.getName());
                item.put("role", card.getRole());
                item.put("gender", card.getGender());
                item.put("age", card.getAge());
                item.put("occupation", card.getOccupation());
                result.add(item);
            }
            return objectMapper.writeValueAsString(Map.of("success", true, "characters", result));
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String executeCreateCharacterCard(String argumentsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});

            CharacterCardEntity card = new CharacterCardEntity();
            card.setName((String) args.get("name"));
            card.setRole((String) args.get("role"));
            card.setGender((String) args.get("gender"));
            if (args.get("age") != null) {
                card.setAge(((Number) args.get("age")).intValue());
            }
            card.setOccupation((String) args.get("occupation"));
            card.setPersonality((String) args.get("personality"));
            card.setBackground((String) args.get("description"));

            // 处理外貌信息 - 存储为JSON和描述文本
            if (args.get("appearance") != null) {
                Map<String, Object> appearance = (Map<String, Object>) args.get("appearance");

                // 存储JSON格式
                card.setAppearanceJson(objectMapper.writeValueAsString(appearance));

                // 构建描述文本用于AI绘图
                StringBuilder appearanceDesc = new StringBuilder();
                if (appearance.get("height") != null) appearanceDesc.append("身高").append(appearance.get("height")).append("，");
                if (appearance.get("hair") != null) appearanceDesc.append(appearance.get("hair")).append("，");
                if (appearance.get("eyes") != null) appearanceDesc.append(appearance.get("eyes")).append("，");
                if (appearance.get("face") != null) appearanceDesc.append(appearance.get("face")).append("，");
                if (appearance.get("build") != null) appearanceDesc.append(appearance.get("build")).append("，");
                if (appearance.get("clothing") != null) appearanceDesc.append("身穿").append(appearance.get("clothing")).append("，");
                if (appearance.get("legwear") != null) appearanceDesc.append(appearance.get("legwear")).append("，");
                if (appearance.get("shoes") != null) appearanceDesc.append(appearance.get("shoes")).append("，");
                if (appearance.get("accessories") != null) appearanceDesc.append("配饰：").append(appearance.get("accessories")).append("，");
                if (appearance.get("distinguishingFeatures") != null) appearanceDesc.append("特征：").append(appearance.get("distinguishingFeatures"));

                // 移除末尾的逗号
                String desc = appearanceDesc.toString();
                if (desc.endsWith("，")) {
                    desc = desc.substring(0, desc.length() - 1);
                }
                card.setAppearanceDescription(desc);
            }

            card = characterCardRepository.save(card);

            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "characterId", card.getId(),
                "message", "角色卡「" + card.getName() + "」创建成功"
            ));
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String executeUpdateCharacterCard(String argumentsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
            Long characterId = ((Number) args.get("characterId")).longValue();

            CharacterCardEntity card = characterCardRepository.findById(characterId)
                .orElseThrow(() -> new RuntimeException("角色卡不存在: " + characterId));

            if (args.get("name") != null) card.setName((String) args.get("name"));
            if (args.get("role") != null) card.setRole((String) args.get("role"));
            if (args.get("gender") != null) card.setGender((String) args.get("gender"));
            if (args.get("age") != null) card.setAge(((Number) args.get("age")).intValue());
            if (args.get("occupation") != null) card.setOccupation((String) args.get("occupation"));
            if (args.get("personality") != null) card.setPersonality((String) args.get("personality"));
            if (args.get("description") != null) card.setBackground((String) args.get("description"));

            if (args.get("appearance") != null) {
                Map<String, Object> appearance = (Map<String, Object>) args.get("appearance");

                // 存储JSON格式
                card.setAppearanceJson(objectMapper.writeValueAsString(appearance));

                // 构建描述文本用于AI绘图
                StringBuilder appearanceDesc = new StringBuilder();
                if (appearance.get("height") != null) appearanceDesc.append("身高").append(appearance.get("height")).append("，");
                if (appearance.get("hair") != null) appearanceDesc.append(appearance.get("hair")).append("，");
                if (appearance.get("eyes") != null) appearanceDesc.append(appearance.get("eyes")).append("，");
                if (appearance.get("face") != null) appearanceDesc.append(appearance.get("face")).append("，");
                if (appearance.get("build") != null) appearanceDesc.append(appearance.get("build")).append("，");
                if (appearance.get("clothing") != null) appearanceDesc.append("身穿").append(appearance.get("clothing")).append("，");
                if (appearance.get("legwear") != null) appearanceDesc.append(appearance.get("legwear")).append("，");
                if (appearance.get("shoes") != null) appearanceDesc.append(appearance.get("shoes")).append("，");
                if (appearance.get("accessories") != null) appearanceDesc.append("配饰：").append(appearance.get("accessories")).append("，");
                if (appearance.get("distinguishingFeatures") != null) appearanceDesc.append("特征：").append(appearance.get("distinguishingFeatures"));

                // 移除末尾的逗号
                String desc = appearanceDesc.toString();
                if (desc.endsWith("，")) {
                    desc = desc.substring(0, desc.length() - 1);
                }
                card.setAppearanceDescription(desc);
            }

            characterCardRepository.save(card);

            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "message", "角色卡更新成功"
            ));
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    // ==================== 其他工具实现 ====================

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

    private String executeCreateVisualWork(String argumentsJson, GenreSession session) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
            String title = (String) args.get("title");
            String visualType = (String) args.get("visualType");
            String description = (String) args.get("description");

            VisualWork work = new VisualWork();
            work.setSessionId(session.getSessionId());
            work.setTitle(title);
            work.setVisualType(visualType);
            work.setDescription(description);
            work = visualWorkRepository.save(work);

            // 更新会话标题
            updateSessionTitleIfNeeded(session, title);

            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "workId", work.getId(),
                "message", "作品《" + title + "》创建成功"
            ));
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String executeAddPanel(String argumentsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
            Long workId = Long.valueOf(args.get("workId").toString());
            Integer panelNumber = Integer.valueOf(args.get("panelNumber").toString());
            String scene = (String) args.get("scene");
            String cameraAngle = (String) args.get("cameraAngle");
            String dialogue = (String) args.get("dialogue");
            String soundEffect = (String) args.get("soundEffect");
            String action = (String) args.get("action");
            String summary = (String) args.get("summary");

            VisualPanel panel = new VisualPanel();
            panel.setWorkId(workId);
            panel.setPanelNumber(panelNumber);
            panel.setScene(scene);
            panel.setCameraAngle(cameraAngle);
            panel.setDialogue(dialogue);
            panel.setSoundEffect(soundEffect);
            panel.setAction(action);
            panel.setSummary(summary);
            panel = visualPanelRepository.save(panel);

            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "panelId", panel.getId(),
                "message", "分镜" + panelNumber + "添加成功"
            ));
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String executeUpdatePanel(String argumentsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
            Long panelId = Long.valueOf(args.get("panelId").toString());
            String scene = (String) args.get("scene");
            String dialogue = (String) args.get("dialogue");
            String summary = (String) args.get("summary");

            VisualPanel panel = visualPanelRepository.findById(panelId)
                .orElseThrow(() -> new RuntimeException("分镜不存在: " + panelId));
            panel.setScene(scene);
            if (dialogue != null) panel.setDialogue(dialogue);
            if (summary != null) panel.setSummary(summary);
            visualPanelRepository.save(panel);

            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "message", "分镜更新成功"
            ));
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String executeGetPanelSummaries(String argumentsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
            Long workId = Long.valueOf(args.get("workId").toString());

            List<VisualPanel> panels = visualPanelRepository.findByWorkIdOrderByPanelNumberAsc(workId);
            List<Map<String, Object>> summaries = new ArrayList<>();
            for (VisualPanel panel : panels) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("panelId", panel.getId());
                item.put("panelNumber", panel.getPanelNumber());
                item.put("cameraAngle", panel.getCameraAngle());
                item.put("summary", panel.getSummary() != null ? panel.getSummary() :
                    panel.getScene().substring(0, Math.min(100, panel.getScene().length())));
                summaries.add(item);
            }

            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "panels", summaries
            ));
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String executeGenerateStoryboard(String argumentsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
            Long workId = Long.valueOf(args.get("workId").toString());
            String storyboard = (String) args.get("storyboard");

            VisualWork work = visualWorkRepository.findById(workId)
                .orElseThrow(() -> new RuntimeException("作品不存在: " + workId));
            work.setStoryboard(storyboard);
            visualWorkRepository.save(work);

            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "message", "分镜脚本生成成功"
            ));
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String executeUpdateVisualWork(String argumentsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
            Long workId = Long.valueOf(args.get("workId").toString());

            VisualWork work = visualWorkRepository.findById(workId)
                .orElseThrow(() -> new RuntimeException("作品不存在: " + workId));

            if (args.containsKey("title")) work.setTitle((String) args.get("title"));
            if (args.containsKey("description")) work.setDescription((String) args.get("description"));
            if (args.containsKey("visualType")) work.setVisualType((String) args.get("visualType"));
            visualWorkRepository.save(work);

            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "message", "作品信息更新成功"
            ));
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String executeUpdateStoryboard(String argumentsJson) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
            Long workId = Long.valueOf(args.get("workId").toString());
            String storyboard = (String) args.get("storyboard");

            VisualWork work = visualWorkRepository.findById(workId)
                .orElseThrow(() -> new RuntimeException("作品不存在: " + workId));
            work.setStoryboard(storyboard);
            visualWorkRepository.save(work);

            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "message", "分镜脚本更新成功"
            ));
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String executeGeneratePanelImage(String argumentsJson, SseEmitter emitter) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
            Long panelId = Long.valueOf(args.get("panelId").toString());
            String size = args.containsKey("size") ? (String) args.get("size") : "16:9";

            VisualPanel panel = visualPanelRepository.findById(panelId)
                .orElseThrow(() -> new RuntimeException("分镜不存在: " + panelId));

            StringBuilder prompt = new StringBuilder();
            if (panel.getScene() != null && !panel.getScene().isEmpty()) {
                prompt.append(panel.getScene());
            }
            if (panel.getCameraAngle() != null && !panel.getCameraAngle().isEmpty()) {
                prompt.append(", ").append(panel.getCameraAngle()).append(" shot");
            }
            if (panel.getAction() != null && !panel.getAction().isEmpty()) {
                prompt.append(", ").append(panel.getAction());
            }
            prompt.append(", manga panel, comic art, high quality");

            String promptStr = prompt.toString();
            logger.info("生成分镜图片: panelId={}, prompt={}", panelId, promptStr);

            emitter.send(SseEmitter.event()
                .name("image_progress")
                .data(objectMapper.writeValueAsString(Map.of(
                    "taskId", "panel-" + panelId,
                    "status", "started",
                    "progress", 0,
                    "message", "正在为分镜" + panel.getPanelNumber() + "生成图片..."
                ))));

            String taskId = evoLinkImageService.generateImage(promptStr, size, null);

            String imageUrl = null;
            int maxAttempts = 150;
            for (int i = 0; i < maxAttempts; i++) {
                Thread.sleep(2000);
                EvoLinkImageService.TaskStatus status = evoLinkImageService.getTaskStatus(taskId);

                int currentProgress = status.getProgress();
                if (i % 10 == 0 || "completed".equals(status.getStatus()) || "failed".equals(status.getStatus())) {
                    emitter.send(SseEmitter.event()
                        .name("image_progress")
                        .data(objectMapper.writeValueAsString(Map.of(
                            "taskId", taskId,
                            "status", status.getStatus(),
                            "progress", currentProgress,
                            "message", "分镜" + panel.getPanelNumber() + "生成中..."
                        ))));
                }

                if ("completed".equals(status.getStatus())) {
                    imageUrl = status.getImageUrl();
                    break;
                } else if ("failed".equals(status.getStatus())) {
                    return "{\"success\": false, \"error\": \"图片生成失败: " + status.getError() + "\"}";
                }
            }

            if (imageUrl == null) {
                return "{\"success\": false, \"error\": \"图片生成超时\"}";
            }

            panel.setImageUrl(imageUrl);
            visualPanelRepository.save(panel);

            emitter.send(SseEmitter.event()
                .name("image_result")
                .data(objectMapper.writeValueAsString(Map.of(
                    "imageUrl", imageUrl,
                    "type", "panel",
                    "panelId", panelId,
                    "panelNumber", panel.getPanelNumber()
                ))));

            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "imageUrl", imageUrl,
                "panelId", panelId,
                "panelNumber", panel.getPanelNumber(),
                "message", "分镜" + panel.getPanelNumber() + "图片生成成功"
            ));
        } catch (Exception e) {
            logger.error("生成分镜图片失败: {}", e.getMessage(), e);
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String executeGenerateReferenceImage(String argumentsJson, SseEmitter emitter) {
        try {
            Map<String, Object> args = objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {});
            String prompt = (String) args.get("prompt");
            String size = args.containsKey("size") ? (String) args.get("size") : "1:1";
            String type = args.containsKey("type") ? (String) args.get("type") : "other";

            if (prompt == null || prompt.trim().isEmpty()) {
                return "{\"success\": false, \"error\": \"提示词不能为空\"}";
            }

            logger.info("生成参考图片: type={}, prompt={}", type, prompt);

            emitter.send(SseEmitter.event()
                .name("image_progress")
                .data(objectMapper.writeValueAsString(Map.of(
                    "taskId", "ref-" + System.currentTimeMillis(),
                    "status", "started",
                    "progress", 0,
                    "message", "开始生成参考图片..."
                ))));

            String taskId = evoLinkImageService.generateImage(prompt, size, null);

            String imageUrl = null;
            int maxAttempts = 150;
            for (int i = 0; i < maxAttempts; i++) {
                Thread.sleep(2000);
                EvoLinkImageService.TaskStatus status = evoLinkImageService.getTaskStatus(taskId);

                int currentProgress = status.getProgress();
                if (i % 10 == 0 || "completed".equals(status.getStatus()) || "failed".equals(status.getStatus())) {
                    emitter.send(SseEmitter.event()
                        .name("image_progress")
                        .data(objectMapper.writeValueAsString(Map.of(
                            "taskId", taskId,
                            "status", status.getStatus(),
                            "progress", currentProgress,
                            "message", "参考图生成中..."
                        ))));
                }

                if ("completed".equals(status.getStatus())) {
                    imageUrl = status.getImageUrl();
                    break;
                } else if ("failed".equals(status.getStatus())) {
                    return "{\"success\": false, \"error\": \"图片生成失败: " + status.getError() + "\"}";
                }
            }

            if (imageUrl == null) {
                return "{\"success\": false, \"error\": \"图片生成超时\"}";
            }

            emitter.send(SseEmitter.event()
                .name("image_result")
                .data(objectMapper.writeValueAsString(Map.of(
                    "imageUrl", imageUrl,
                    "type", type,
                    "name", "参考图"
                ))));

            return objectMapper.writeValueAsString(Map.of(
                "success", true,
                "imageUrl", imageUrl,
                "type", type,
                "message", "参考图片生成成功"
            ));
        } catch (Exception e) {
            logger.error("生成参考图片失败: {}", e.getMessage(), e);
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
        params.put("visualType", null);
        params.put("theme", null);
        params.put("style", null);
        params.put("artStyle", null);
        params.put("panelLayout", null);
        params.put("panelCount", 30);
        return params;
    }
}

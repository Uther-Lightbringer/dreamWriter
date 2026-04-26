package org.zenithon.articlecollect.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.zenithon.articlecollect.dto.CharacterCard;
import org.zenithon.articlecollect.dto.CharacterCardAppearance;
import org.zenithon.articlecollect.dto.GeneratedOutline;

import java.util.ArrayList;
import java.util.List;

/**
 * 小说生成器单步生成服务
 * 负责调用AIPromptService生成各种内容
 */
@Service
public class NovelGeneratorStepService {

    private static final Logger logger = LoggerFactory.getLogger(NovelGeneratorStepService.class);

    // DeepSeek v4-flash 最大输出token限制
    private static final int MAX_TOKENS = 8192;

    // 统一的 System Prompt 前缀，用于 DeepSeek 前缀缓存优化
    private static final String SYSTEM_PREFIX = "你是一名专业的成人小说创作助手，擅长创作具有张力和吸引力的故事内容。你精通各种文学技巧，能够根据用户的需求创作出高质量的小说内容。\n\n---\n\n";

    private final AIPromptService aiPromptService;
    private final ObjectMapper objectMapper;

    // ThreadLocal 存储当前任务使用的模型
    private final ThreadLocal<String> currentModel = new ThreadLocal<>();

    /**
     * 设置当前任务使用的模型
     */
    public void setCurrentModel(String model) {
        currentModel.set(model);
        logger.info("设置当前任务模型: {}", model);
    }

    /**
     * 获取当前任务使用的模型
     */
    public String getCurrentModel() {
        return currentModel.get();
    }

    /**
     * 清除当前任务的模型设置
     */
    public void clearCurrentModel() {
        currentModel.remove();
    }

    public NovelGeneratorStepService(AIPromptService aiPromptService, ObjectMapper objectMapper) {
        this.aiPromptService = aiPromptService;
        this.objectMapper = objectMapper;
    }

    /**
     * 调用 DeepSeek API（使用当前任务设置的模型）
     */
    private String callAPI(String systemPrompt, String userPrompt,
                           boolean enableThinking, String reasoningEffort, int maxTokens) throws Exception {
        String model = currentModel.get();
        if (model != null && !model.isEmpty()) {
            return aiPromptService.callDeepSeekAPIWithModel(systemPrompt, userPrompt,
                enableThinking, reasoningEffort, maxTokens, false, model);
        } else {
            // 当未设置模型时，使用默认模型调用
            return aiPromptService.callDeepSeekAPIWithThinking(systemPrompt, userPrompt,
                enableThinking, reasoningEffort, maxTokens, false);
        }
    }

    /**
     * 调用 DeepSeek API（使用当前任务设置的模型，支持 JSON 模式）
     */
    private String callAPIWithJsonRetry(String systemPrompt, String userPrompt,
                                         boolean enableThinking, String reasoningEffort,
                                         int maxTokens, int maxRetries) throws Exception {
        String model = currentModel.get();
        if (model != null && !model.isEmpty()) {
            // 对于带模型参数的 JSON 重试调用
            String currentPrompt = userPrompt;
            String lastError = null;
            int attempt = 0;

            while (attempt < maxRetries) {
                attempt++;
                logger.info("JSON生成尝试 {}/{}, 模型: {}", attempt, maxRetries, model);

                try {
                    String response = aiPromptService.callDeepSeekAPIWithModel(
                        systemPrompt, currentPrompt,
                        enableThinking, reasoningEffort,
                        maxTokens, true, model
                    );

                    if (response == null || response.trim().isEmpty()) {
                        lastError = "API返回空响应";
                        logger.warn("尝试 {}: {}", attempt, lastError);
                        continue;
                    }

                    // 验证JSON格式
                    objectMapper.readTree(response);
                    logger.info("JSON验证成功，尝试次数: {}", attempt);
                    return response;

                } catch (Exception e) {
                    lastError = e.getMessage();
                    logger.warn("尝试 {} JSON解析失败: {}", attempt, lastError);
                    // 构建重试提示
                    if (attempt < maxRetries) {
                        currentPrompt = userPrompt + "\n\n上次生成失败，错误：" + lastError +
                            "。请严格按照JSON格式输出，不要包含任何其他内容。";
                    }
                }
            }
            throw new RuntimeException("JSON生成失败，已重试" + maxRetries + "次: " + lastError);
        } else {
            return aiPromptService.callDeepSeekAPIWithJsonRetry(systemPrompt, userPrompt,
                enableThinking, reasoningEffort, maxTokens, maxRetries);
        }
    }

    // ==================== Step 1: 工具生成 ====================

    /**
     * 生成工具列表
     * @param toolsType 工具类型
     * @return 生成的工具列表文本
     */
    public String generateTools(String toolsType) throws Exception {
        logger.info("开始生成工具列表, 类型: {}", toolsType);

        String systemPrompt = SYSTEM_PREFIX + "输出和" + toolsType + "的玩法相关的道具，以及这些道具的玩法。道具的数量要有20种，不可以重复。工具都要使用中文。\n\n" +
            "以下是要生成的格式的。其中「用法」的字数不得超过20。\n\n" +
            "①「工具」：「用法」；\n\n" +
            "②「工具」：「用法」；";

        String result = callAPI(systemPrompt, null, false, null, MAX_TOKENS);
        logger.info("工具列表生成完成, 长度: {}", result.length());
        return result;
    }

    // ==================== Step 2: 玩法生成 ====================

    /**
     * 生成玩法列表
     * @param gameplayType 玩法类型
     * @return 生成的玩法列表文本
     */
    public String generateGameplay(String gameplayType) throws Exception {
        logger.info("开始生成玩法列表, 类型: {}", gameplayType);

        String systemPrompt = SYSTEM_PREFIX + "输出和" + gameplayType + "的玩法，要有20种，不可以重复。玩法都要使用中文。\n\n" +
            "以下是要生成的格式的。其中解释字数不能超过30。\n\n" +
            "①玩法：解释\n\n" +
            "②玩法：解释";

        String result = callAPI(systemPrompt, null, false, null, MAX_TOKENS);
        logger.info("玩法列表生成完成, 长度: {}", result.length());
        return result;
    }

    // ==================== Step 3: 世界观生成 ====================

    /**
     * 生成世界观
     * @param keyword 关键词/故事大纲
     * @return 生成的世界观文本
     */
    public String generateWorldview(String keyword) throws Exception {
        logger.info("开始生成世界观");

        String systemPrompt = SYSTEM_PREFIX + "基于以下关键词，使用参照以下内容，生成世界观。要严格按照markdown格式。不需要完全拘泥于以下的模板。输出的内容里面只需要包括世界观内容，不要包含其他的任何东西。";

        String result = callAPI(systemPrompt, keyword, true, "high", MAX_TOKENS);
        logger.info("世界观生成完成, 长度: {}", result.length());
        return result;
    }

    // ==================== Step 4: 角色卡生成 ====================

    /**
     * 生成角色卡列表
     * @param keyword 关键词
     * @param worldview 世界观
     * @param roleCount 角色数量
     * @param tools 工具列表
     * @param gameplay 玩法列表
     * @param protagonist 主角名称
     * @return 角色卡列表
     */
    public List<CharacterCard> generateCharacterCards(String keyword, String worldview,
                                                       int roleCount, String tools,
                                                       String gameplay, String protagonist) throws Exception {
        logger.info("开始生成角色卡, 数量: {}", roleCount);

        String systemPrompt = SYSTEM_PREFIX + "根据世界观，生成在这个世界观下的故事的人物,后面要根据这些人物来写小说。生成" + roleCount + "个角色。可以包含以下内容来生成角色卡。不需要严格按照以下的格式。输出的内容里面只需要包括角色卡内容，不要包含其他的任何东西。\n" +
            "生成的角色要围绕：关键词\n" +
            "可以附带的工具：" + tools + "\n" +
            "可以附带的玩法:" + gameplay + "\n" +
            "主角:" + protagonist + "\n" +
            "生成的角色卡的顺序，要把主角放在第一。\n" +
            "生成的角色名称，要以中国人的命名的风格来。人种要以中国人为主。\n\n" +
            "【外貌描写要求 - 重要】\n" +
            "外貌描写用于AI生成角色图片，必须准确、具体，禁止使用比喻：\n" +
            "- 眼睛：描述眼型（丹凤眼、桃花眼、杏眼等）、瞳色、眼尾特点\n" +
            "- 眉毛：描述眉形（柳叶眉、剑眉、平眉等）、粗细、颜色\n" +
            "- 鼻子：描述鼻梁高度、鼻头大小\n" +
            "- 嘴巴：描述唇形、唇色、厚薄\n" +
            "- 脸型：描述脸型（瓜子脸、圆脸、方脸等）、下颌线条\n" +
            "- 发型：描述长度、颜色、卷直、造型\n" +
            "- 身高：给出具体数字\n" +
            "- 体型：描述身材特点\n" +
            "- 穿着：描述日常服装风格、配饰\n\n" +
            "【错误示例】\"美若天仙\"\"眼睛像星星\"（太抽象或比喻）\n" +
            "【正确示例】\"丹凤眼，深褐色瞳孔，眼尾微微上挑。柳叶眉。瓜子脸。齐腰黑发，微卷。身高168cm。穿着白色衬衫配淡蓝色长裙。\"\n\n" +
            "生成的内容里面出现的所有标点符号，都要使用英文输入法的。\n" +
            "要求输出的格式严格按照以下的JSON来：\n\n" +
            "[\n" +
            "  {\n" +
            "    \"name\": \"角色姓名\",\n" +
            "    \"role\": \"protagonist\",\n" +
            "    \"alternativeNames\": [\"别名1\", \"别名2\"],\n" +
            "    \"age\": 0,\n" +
            "    \"gender\": \"性别\",\n" +
            "    \"occupation\": \"职业\",\n" +
            "    \"appearance\": {\n" +
            "      \"height\": \"身高\",\n" +
            "      \"hair\": \"发色和发型\",\n" +
            "      \"eyes\": \"眼睛描述\",\n" +
            "      \"face\": \"脸型描述\",\n" +
            "      \"build\": \"体型\",\n" +
            "      \"distinguishingFeatures\": \"面部特征（眼睛、眉毛、鼻子、嘴巴的综合描述）\",\n" +
            "      \"clothing\": \"穿着风格\"\n" +
            "    },\n" +
            "    \"appearanceDescription\": \"完整的外貌文字描述，用于AI生成图片\",\n" +
            "    \"personality\": \"性格描述\",\n" +
            "    \"background\": \"背景故事\",\n" +
            "    \"relationships\": [\n" +
            "      {\n" +
            "        \"targetName\": \"其他角色姓名\",\n" +
            "        \"relationship\": \"关系描述\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"notes\": \"备注\"\n" +
            "  }\n" +
            "]";

        String userPrompt = "关键词：" + keyword + "\n\n世界观：" + worldview;

        // 启用JSON模式并带重试机制确保输出有效的JSON
        String result = aiPromptService.callDeepSeekAPIWithJsonRetry(systemPrompt, userPrompt, true, "high", MAX_TOKENS, 3);

        // 解析JSON为角色卡列表
        List<CharacterCard> cards = parseCharacterCards(result);
        logger.info("角色卡生成完成, 数量: {}", cards.size());
        return cards;
    }

    /**
     * 解析角色卡JSON
     */
    private List<CharacterCard> parseCharacterCards(String json) throws Exception {
        List<CharacterCard> cards = new ArrayList<>();

        // 尝试提取JSON数组
        String jsonArray = extractJsonArray(json);

        if (jsonArray == null) {
            logger.warn("无法从响应中提取JSON数组，尝试直接解析");
            jsonArray = json;
        }

        try {
            JsonNode arrayNode = objectMapper.readTree(jsonArray);
            if (arrayNode.isArray()) {
                for (JsonNode node : arrayNode) {
                    CharacterCard card = new CharacterCard();
                    card.setName(node.path("name").asText());
                    card.setRole(node.path("role").asText());  // 角色类型
                    card.setAge(node.path("age").asInt(0));
                    card.setGender(node.path("gender").asText());
                    card.setOccupation(node.path("occupation").asText());
                    card.setPersonality(node.path("personality").asText());
                    card.setBackground(node.path("background").asText());
                    card.setNotes(node.path("notes").asText());

                    // 外貌文字描述（用于AI生成图片）
                    card.setAppearanceDescription(node.path("appearanceDescription").asText());

                    // 解析外貌结构
                    JsonNode appearanceNode = node.path("appearance");
                    if (!appearanceNode.isMissingNode()) {
                        CharacterCardAppearance appearance = new CharacterCardAppearance();
                        appearance.setHeight(appearanceNode.path("height").asText());
                        appearance.setHair(appearanceNode.path("hair").asText());
                        appearance.setEyes(appearanceNode.path("eyes").asText());
                        appearance.setFace(appearanceNode.path("face").asText());
                        appearance.setBuild(appearanceNode.path("build").asText());
                        appearance.setDistinguishingFeatures(appearanceNode.path("distinguishingFeatures").asText());
                        appearance.setClothing(appearanceNode.path("clothing").asText());
                        card.setAppearance(appearance);
                    }

                    cards.add(card);
                }
            }
        } catch (Exception e) {
            logger.error("解析角色卡JSON失败: {}", e.getMessage());
            throw new RuntimeException("解析角色卡失败: " + e.getMessage());
        }

        return cards;
    }

    /**
     * 从文本中提取JSON数组 [...]
     */
    private String extractJsonArray(String text) {
        if (text == null) return null;

        // 找到第一个 [ 和匹配的 ]
        int start = text.indexOf('[');
        if (start == -1) return null;

        int depth = 0;
        int end = -1;
        boolean inString = false;
        boolean escaped = false;

        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == '[') depth++;
                else if (c == ']') {
                    depth--;
                    if (depth == 0) {
                        end = i;
                        break;
                    }
                }
            }
        }

        if (end > start) {
            return text.substring(start, end + 1);
        }

        // 如果没有找到匹配的 ]，返回从 [ 开始到末尾的内容
        logger.warn("JSON数组未正确闭合，返回从 [ 开始的所有内容");
        return text.substring(start);
    }

    // ==================== Step 5: 大纲和章节生成 ====================

    /**
     * 生成大纲和章节列表
     * @param keyword 关键词
     * @param worldview 世界观
     * @param charactersJson 角色卡JSON
     * @param chapterCount 章节数量
     * @param genre 体裁
     * @param requires 扩写需求
     * @param tools 工具
     * @param gameplay 玩法
     * @param pointOfView 叙述视角
     * @param protagonist 主角
     * @return 大纲结果
     */
    public GeneratedOutline generateOutline(String keyword, String worldview, String charactersJson,
                                            int chapterCount, String genre, String requires,
                                            String tools, String gameplay, String pointOfView,
                                            String protagonist) throws Exception {
        logger.info("开始生成大纲和章节列表, 章节数: {}", chapterCount);

        String systemPrompt = SYSTEM_PREFIX + "根据关键词得出来的内容，我需要进行续扩写。对故事的扩写保持输入的内容的基本基调，不要偏离基调。\n" +
            "文章中要加入更多具体的例子、生动的描述和感官细节等。使用具体的例子和生动的故事。\n" +
            "调教工具：" + tools + "\n" +
            "玩法：" + gameplay + "\n" +
            "世界观:" + worldview + "\n" +
            "可用的人物角色卡:" + charactersJson + "\n" +
            "叙述视角：" + pointOfView + "\n" +
            "主角:" + protagonist;

        String userPrompt = "先生成整个故事的大纲，然后帮我生成" + chapterCount + "个章节，章节标题和章节概括。\n\n" +
            "根据" + keyword + "来创作一个故事。\n\n" +
            "# 生成范式：\n" +
            " 章节标题要准确，概括章节的主要内容。标题要口语化。\n" +
            "1. 章节概括要求不得少于100字。章节概括不要带有任何总结的话语，仅需描写动作和事实即可。\n" +
            "2. 设计各个章节之间的时候要有联系。后面的章节可以适度地回应前文。\n" +
            "3 . 在个别章节，可以适当地带出和显示角色的工作等。\n" +
            "必须严格按照以下JSON格式输出：\n" +
            "{\n" +
            "  \"outline\": \"故事大纲内容\",\n" +
            "  \"chapters\": [\n" +
            "    {\"section\": \"章节标题\", \"description\": \"章节概括\"}\n" +
            "  ]\n" +
            "}";

        // 启用JSON模式并带重试机制确保输出有效的JSON
        String result = aiPromptService.callDeepSeekAPIWithJsonRetry(systemPrompt, userPrompt, true, "max", MAX_TOKENS, 3);

        // 解析大纲和章节
        GeneratedOutline outline = parseOutline(result);
        logger.info("大纲生成完成, 章节数: {}", outline.getChapters() != null ? outline.getChapters().size() : 0);
        return outline;
    }

    /**
     * 解析大纲JSON
     */
    private GeneratedOutline parseOutline(String json) throws Exception {
        GeneratedOutline outline = new GeneratedOutline();

        try {
            // 记录原始响应用于调试
            logger.info("AI返回的原始内容长度: {} 字符", json != null ? json.length() : 0);

            if (json == null || json.trim().isEmpty()) {
                throw new RuntimeException("AI返回的内容为空");
            }

            // 先尝试从文本中提取JSON对象
            String jsonContent = extractJsonObject(json);

            if (jsonContent == null) {
                // 如果没有找到JSON对象，尝试提取JSON数组
                jsonContent = extractJsonArray(json);
            }

            if (jsonContent == null) {
                logger.error("无法从响应中提取JSON内容");
                logger.error("响应内容前500字符: {}", json.length() > 500 ? json.substring(0, 500) : json);
                throw new RuntimeException("AI返回的内容中未找到有效的JSON格式");
            }

            // 尝试修复常见的JSON问题
            jsonContent = repairJson(jsonContent);

            logger.info("提取到的JSON内容长度: {} 字符", jsonContent.length());
            logger.debug("提取到的JSON内容: {}", jsonContent.length() > 1000 ? jsonContent.substring(0, 1000) + "..." : jsonContent);

            JsonNode rootNode = objectMapper.readTree(jsonContent);

            // 提取大纲
            if (rootNode.has("outline")) {
                outline.setOutline(rootNode.get("outline").asText());
            }

            // 提取章节列表 - 支持 "chapters" 或 "chapter" 字段
            JsonNode chaptersNode = null;
            if (rootNode.has("chapters")) {
                chaptersNode = rootNode.get("chapters");
            } else if (rootNode.has("chapter")) {
                chaptersNode = rootNode.get("chapter");
            }

            if (chaptersNode != null && chaptersNode.isArray()) {
                List<GeneratedOutline.ChapterInfo> chapters = new ArrayList<>();
                for (JsonNode node : chaptersNode) {
                    GeneratedOutline.ChapterInfo chapter = new GeneratedOutline.ChapterInfo();
                    chapter.setSection(node.path("section").asText());
                    chapter.setDescription(node.path("description").asText());
                    chapters.add(chapter);
                }
                outline.setChapters(chapters);
            } else if (rootNode.isArray()) {
                // 如果直接是数组，当作章节列表处理
                List<GeneratedOutline.ChapterInfo> chapters = new ArrayList<>();
                for (JsonNode node : rootNode) {
                    GeneratedOutline.ChapterInfo chapter = new GeneratedOutline.ChapterInfo();
                    chapter.setSection(node.path("section").asText());
                    chapter.setDescription(node.path("description").asText());
                    chapters.add(chapter);
                }
                outline.setChapters(chapters);
            }

            // 如果没有提取到章节，记录警告
            if (outline.getChapters() == null || outline.getChapters().isEmpty()) {
                logger.warn("未能从JSON中提取到章节列表");
            }

        } catch (Exception e) {
            logger.error("解析大纲JSON失败: {}", e.getMessage());
            // 打印更多上下文帮助调试
            if (json != null && json.length() > 0) {
                logger.error("原始JSON前1000字符: {}", json.length() > 1000 ? json.substring(0, 1000) : json);
            }
            throw new RuntimeException("解析大纲失败: " + e.getMessage());
        }

        return outline;
    }

    /**
     * 尝试修复常见的JSON问题
     */
    private String repairJson(String json) {
        if (json == null) return null;

        String repaired = json.trim();

        // 计算当前括号状态
        int braceCount = 0;   // { }
        int bracketCount = 0; // [ ]
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < repaired.length(); i++) {
            char c = repaired.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (!inString) {
                if (c == '{') braceCount++;
                else if (c == '}') braceCount--;
                else if (c == '[') bracketCount++;
                else if (c == ']') bracketCount--;
            }
        }

        // 如果括号已经平衡，直接返回
        if (braceCount == 0 && bracketCount == 0 && !inString) {
            return repaired;
        }

        logger.warn("JSON格式不完整，尝试修复... braceCount={}, bracketCount={}, inString={}", braceCount, bracketCount, inString);

        // 如果在字符串中间截断，先关闭字符串
        if (inString) {
            repaired += "\"";
        }

        // 补全未闭合的括号（注意顺序：先关数组，再关对象）
        // 但要根据实际的嵌套关系来决定顺序
        // 简单处理：先补 ] 再补 }
        for (int i = 0; i < bracketCount; i++) {
            repaired += "]";
        }
        for (int i = 0; i < braceCount; i++) {
            repaired += "}";
        }

        logger.info("JSON修复完成，添加了 {} 个 ] 和 {} 个 }}", bracketCount, braceCount);

        return repaired;
    }

    /**
     * 从文本中提取JSON对象 {...}
     */
    private String extractJsonObject(String text) {
        if (text == null) return null;

        // 找到第一个 { 和匹配的 }
        int start = text.indexOf('{');
        if (start == -1) return null;

        int depth = 0;
        int end = -1;
        boolean inString = false;
        boolean escaped = false;

        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        end = i;
                        break;
                    }
                }
            }
        }

        if (end > start) {
            return text.substring(start, end + 1);
        }

        // 如果没有找到匹配的 }，返回从 { 开始到末尾的内容
        logger.warn("JSON对象未正确闭合，返回从 {{ 开始的所有内容");
        return text.substring(start);
    }

    // ==================== Step 6: 生成小说标题 ====================

    /**
     * 生成小说标题
     * @param outline 大纲
     * @param pointOfView 叙述视角
     * @return 小说标题
     */
    public String generateNovelTitle(String outline, String pointOfView) throws Exception {
        logger.info("开始生成小说标题");

        String systemPrompt = SYSTEM_PREFIX + "生成这个大纲和标题所代表的小说名字。只需要说出标题即可，标题可以带有隐喻和概括。生成的小说标题不要带有任何标点符号。叙述视角：" + pointOfView;

        String result = callAPI(systemPrompt, outline, false, null, MAX_TOKENS);
        // 清理可能的引号
        result = result.replaceAll("[\"'「」『』【】]", "").trim();
        logger.info("小说标题生成完成: {}", result);
        return result;
    }

    // ==================== Step 7: 章节扩写 ====================

    /**
     * 提取当前章节需要的角色和世界观
     * @param allCharacters 所有角色卡JSON
     * @param worldview 世界观
     * @param chapterInfo 当前章节信息
     * @return 精简后的角色和世界观描述
     */
    public String extractRelevantContext(String allCharacters, String worldview,
                                          GeneratedOutline.ChapterInfo chapterInfo) throws Exception {
        logger.debug("开始提取章节相关上下文");

        String systemPrompt = SYSTEM_PREFIX + "你是一个人物角色总结大师。我下面给出了人物的角色卡、世界观和故事的章节概括。帮我基于章节概括里面出现的人物和世界观，只抽取在本章节内需要体现的特质。我的目的是缩小人物和世界观的所占的上下文，然后结合章节概括，提供给另一个AI来扩展生成符合人物和世界观的小说。\n\n" +
            "按照以下模板生成：\n\n" +
            "  基于以下角色卡和故事背景，生成[章节内容/对话/情节发展]：\n\n" +
            "  **故事背景**：[简要描述世界观、当前阶段]\n\n" +
            "  **角色卡参考**：\n" +
            "  1. [角色A姓名]：[关键特质摘要]\n" +
            "  2. [角色B姓名]：[关键特质摘要]\n\n" +
            "  **当前情境**：[具体场景、冲突、需要解决的问题]\n\n" +
            "  **生成要求**：\n" +
            "  - 保持角色性格一致性\n" +
            "  - 符合角色能力和限制\n" +
            "  - 推动预设的角色发展弧线";

        String userPrompt = "故事的角色卡：" + allCharacters + "\n\n世界观：" + worldview + "\n\n当前的故事章节和大纲：" +
            chapterInfo.getSection() + "\n" + chapterInfo.getDescription();

        String result = callAPI(systemPrompt, userPrompt, true, "high", MAX_TOKENS);
        return result;
    }

    /**
     * 扩写章节
     * @param chapterInfo 章节信息
     * @param outline 整体大纲
     * @param relevantContext 相关上下文
     * @param tools 工具
     * @param gameplay 玩法
     * @param genre 体裁
     * @param requires 扩写需求
     * @param pointOfView 叙述视角
     * @param languageStyle 语言风格
     * @param wordsPerChapter 每章字数要求
     * @return 扩写后的章节内容
     */
    public String expandChapter(GeneratedOutline.ChapterInfo chapterInfo, String outline,
                                String relevantContext, String tools, String gameplay,
                                String genre, String requires, String pointOfView,
                                String languageStyle, int wordsPerChapter) throws Exception {
        logger.info("开始扩写章节: {}", chapterInfo.getSection());

        String systemPrompt = SYSTEM_PREFIX + "女王可以使用的工具：" + tools + "\n" +
            "玩法：" + gameplay + "\n" +
            requires + "\n" +
            "# 以" + genre + "的格式进行描写。\n" +
            "体裁: " + genre + "\n" +
            "语言风格：" + languageStyle + "\n" +
            "每章字数要求：" + wordsPerChapter + "字以上\n" +
            "故事扩写里面的人物要严格按照角色卡进行：" + relevantContext + "\n" +
            "叙述视角：" + pointOfView;

        String userPrompt = "## 整体大纲\n" + outline + "\n## 需要扩展的段落\n" +
            chapterInfo.getSection() + "\n" + chapterInfo.getDescription() + "\n\n" +
            "生成的内容的第一句要标题开头。然后换行。\n" +
            "如：\n" +
            "```\n" +
            "# 第一章： xxxx\n" +
            "   xxxxx\n\n" +
            "```";

        String result = callAPI(systemPrompt, userPrompt, true, "max", MAX_TOKENS);
        logger.info("章节扩写完成, 长度: {}", result.length());
        return result;
    }

    // ==================== Step 8: 去除AI味润色 ====================

    /**
     * 去除AI味润色
     * @param content 原始内容
     * @return 润色后的内容
     */
    public String polishContent(String content, int wordsPerChapter) throws Exception {
        logger.info("开始去除AI味润色");

        String systemPrompt = SYSTEM_PREFIX + "# 质量检查清单\n" +
            "需要对传入的扩写内容进行检查，并修改。检查完之后修改对应的内容，更符合清单。结果只输出修改后的文章,不要附带其他任何东西。。\n" +
            "请用以下规范输出：\n" +
            "1.语言平实直述，避免抽象隐喻；\n" +
            "2.使用日常场景化案例辅助说明；\n" +
            "3.优先选择具体名词替代抽象概念；\n" +
            "4.保持段落简明（不超过5行）；\n" +
            "5.技术表述需附通俗解释；\n" +
            "6.禁用文学化修辞；\n" +
            "7.重点信息前置；\n" +
            "8.复杂内容分点说明；\n" +
            "9.保持口语化但不过度简化专业内容；\n" +
            "10.确保信息准确前提下优先选择大众认知词汇\n" +
            "交付章节前使用此清单确保质量。\n\n" +
            "**深度润色（去除AI味）** - 重点检查并修改：\n" +
            "    - **去除过度修饰的形容词**：删减「璀璨」、「瑰丽」、「绚烂」等AI常用词堆砌\n" +
            "    - **减少抽象陈述**：把「心中涌起复杂的情感」改为具体动作/对话\n" +
            "    - **打破四字格律**：避免「心潮澎湃、热血沸腾」等陈词滥调\n" +
            "    - **增加口语化表达**：人物对话要有个性，避免「书面语套话」\n" +
            "    - **优化节奏感**：长句和短句交替，避免句式单调\n" +
            "    - **细节具象化**：用具体的视觉/听觉/嗅觉细节替代笼统描述\n" +
            "    - **去掉「记住」**： 去掉类似「记住xxx」这样的对话，替换成更贴近人的话语\n" +
            "    - **人物对话不要有陈述句**\n\n" +
            "字数不得少于" + wordsPerChapter + "字。";

        String result = callAPI(systemPrompt, content, true, "high", MAX_TOKENS);
        logger.info("润色完成, 长度: {}", result.length());
        return result;
    }

    /**
     * 对已有内容进行续写扩写
     * @param content 已有内容
     * @param needWords 需要增加的字数
     * @param languageStyle 语言风格
     * @return 扩写后的内容
     */
    public String expandContent(String content, int needWords, String languageStyle) throws Exception {
        logger.info("开始续写扩写，需增加约 {} 字", needWords);

        String systemPrompt = SYSTEM_PREFIX + "你是一名专业的小说作家。当前文章字数不足，需要你进行续写扩写。\n" +
            "语言风格：" + languageStyle + "\n\n" +
            "要求：\n" +
            "1. 继续文章内容，自然衔接，不要重复已有内容\n" +
            "2. 增加至少 " + needWords + " 字的新内容\n" +
            "3. 保持故事情节连贯，人物性格一致\n" +
            "4. 可以增加更多细节描写、对话、场景描述\n" +
            "5. 输出完整的新文章（包括原有内容+新增内容）\n" +
            "6. 不要在开头或结尾添加任何说明";

        String userPrompt = "以下是现有文章内容，请继续扩写：\n\n" + content;

        String result = callAPI(systemPrompt, userPrompt, true, "max", MAX_TOKENS);
        logger.info("续写扩写完成, 长度: {}", result.length());
        return result;
    }

    /**
     * 格式化章节内容（句号后换行）
     */
    public String formatChapterContent(String content) {
        if (content == null) return null;
        // 在每个中文句号后面添加一个换行符
        return content.replaceAll("。", "。\n");
    }
}

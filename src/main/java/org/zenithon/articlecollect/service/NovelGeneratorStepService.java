package org.zenithon.articlecollect.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.zenithon.articlecollect.config.DeepSeekConfig;
import org.zenithon.articlecollect.dto.CharacterCard;
import org.zenithon.articlecollect.dto.CharacterCardAppearance;
import org.zenithon.articlecollect.dto.GeneratedOutline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 小说生成器单步生成服务
 * 负责调用DeepSeek API生成各种内容
 */
@Service
public class NovelGeneratorStepService {

    private static final Logger logger = LoggerFactory.getLogger(NovelGeneratorStepService.class);

    private final DeepSeekConfig deepSeekConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public NovelGeneratorStepService(DeepSeekConfig deepSeekConfig, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.deepSeekConfig = deepSeekConfig;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 调用DeepSeek API
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户提示词
     * @param enableThinking 是否开启思考模式
     * @param reasoningEffort 思考强度 (high/max)
     * @param maxTokens 最大token数
     * @return AI返回的内容
     */
    public String callDeepSeekAPI(String systemPrompt, String userPrompt,
                                   boolean enableThinking, String reasoningEffort,
                                   int maxTokens) throws Exception {
        // 检查 API Key
        if (deepSeekConfig.getApiKey() == null || deepSeekConfig.getApiKey().trim().isEmpty()) {
            throw new RuntimeException("DeepSeek API Key 未配置");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + deepSeekConfig.getApiKey());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", deepSeekConfig.getModel());
        requestBody.put("max_tokens", maxTokens);

        // 思考模式配置
        if (enableThinking) {
            Map<String, Object> thinking = new HashMap<>();
            thinking.put("type", "enabled");
            requestBody.put("thinking", thinking);
            if (reasoningEffort != null) {
                requestBody.put("reasoning_effort", reasoningEffort);
            }
        }

        // 构建消息
        List<Map<String, String>> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);
        }
        if (userPrompt != null && !userPrompt.isEmpty()) {
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);
            messages.add(userMessage);
        }
        requestBody.put("messages", messages);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        logger.debug("调用DeepSeek API, thinking={}, maxTokens={}", enableThinking, maxTokens);
        ResponseEntity<String> response = restTemplate.postForEntity(
            deepSeekConfig.getApiUrl(),
            request,
            String.class
        );

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode choices = rootNode.path("choices");

            if (choices.isArray() && choices.size() > 0) {
                JsonNode messageNode = choices.get(0).path("message");
                String content = messageNode.path("content").asText();

                // 如果开启了思考模式，可能需要清理reasoning_content
                // DeepSeek v4-flash会在响应中包含reasoning_content字段
                // 但最终内容在content字段中，已经去掉了<think>标签

                if (content != null && !content.isEmpty()) {
                    // 清理可能的<think>标签
                    content = cleanThinkTags(content);
                    return content.trim();
                }
            }
        }

        throw new RuntimeException("API调用失败: " + response.getStatusCode());
    }

    /**
     * 清理<think>标签
     */
    private String cleanThinkTags(String content) {
        if (content == null) return null;
        // 移除<think>...</think>标签及其内容
        return content.replaceAll("<think>.*?</think>", "").trim();
    }

    // ==================== Step 1: 工具生成 ====================

    /**
     * 生成工具列表
     * @param toolsType 工具类型
     * @return 生成的工具列表文本
     */
    public String generateTools(String toolsType) throws Exception {
        logger.info("开始生成工具列表, 类型: {}", toolsType);

        String systemPrompt = "输出和" + toolsType + "的玩法相关的道具，以及这些道具的玩法。道具的数量要有20种，不可以重复。工具都要使用中文。\n\n" +
            "以下是要生成的格式的。其中"用法"的字数不得超过20。\n\n" +
            "①"工具":"用法"；\n\n" +
            "②"工具":"用法"；";

        String result = callDeepSeekAPI(systemPrompt, null, false, null, 2000);
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

        String systemPrompt = "输出和" + gameplayType + "的玩法，要有20种，不可以重复。玩法都要使用中文。\n\n" +
            "以下是要生成的格式的。其中解释字数不能超过30。\n\n" +
            "①玩法：解释\n\n" +
            "②玩法：解释";

        String result = callDeepSeekAPI(systemPrompt, null, false, null, 2000);
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

        String systemPrompt = "基于以下关键词，使用参照以下内容，生成世界观。要严格按照markdown格式。不需要完全拘泥于以下的模板。输出的内容里面只需要包括世界观内容，不要包含其他的任何东西。";

        String result = callDeepSeekAPI(systemPrompt, keyword, true, "high", 4000);
        result = cleanThinkTags(result);
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

        String systemPrompt = "根据世界观，生成在这个世界观下的故事的人物,后面要根据这些人物来写小说。生成" + roleCount + "个角色。可以包含以下内容来生成角色卡。不需要严格按照以下的格式。输出的内容里面只需要包括角色卡内容，不要包含其他的任何东西。\n" +
            "生成的角色要围绕：关键词\n" +
            "可以附带的工具：" + tools + "\n" +
            "可以附带的玩法:" + gameplay + "\n" +
            "主角:" + protagonist + "\n" +
            "生成的角色卡的顺序，要把主角放在第一。\n" +
            "生成的角色名称，要以中国人的命名的风格来。人种要以中国人为主。可以加入该人物喜欢的衣服和穿戴特色。生成的内容里面出现的所有标点符号，都要使用英文输入法的。她/他的眼睛、眉毛、鼻子、嘴巴、脸型、身高、衣服、发型和整体风格都要描述出来，尽量不用比喻，而是要准确的白描。这是为了能把文字提供给AI生成图片的时候，生成同一个人物的时候脸保持一致。\n" +
            "要求输出的格式严格按照以下的JSON来：\n\n" +
            "[\n" +
            "  {\n" +
            "    \"name\": \"角色姓名\",\n" +
            "    \"alternativeNames\": [\"别名1\", \"别名2\"],\n" +
            "    \"age\": 0,\n" +
            "    \"gender\": \"性别\",\n" +
            "    \"occupation\": \"职业\",\n" +
            "    \"appearance\": {\n" +
            "      \"height\": \"身高\",\n" +
            "      \"hair\": \"发色\",\n" +
            "      \"eyes\": \"瞳色\",\n" +
            "      \"build\": \"体型\",\n" +
            "      \"distinguishingFeatures\": \"特征\"\n" +
            "    },\n" +
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

        String result = callDeepSeekAPI(systemPrompt, userPrompt, true, "high", 8000);
        result = cleanThinkTags(result);

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
                    card.setAge(node.path("age").asInt(0));
                    card.setGender(node.path("gender").asText());
                    card.setOccupation(node.path("occupation").asText());
                    card.setPersonality(node.path("personality").asText());
                    card.setBackground(node.path("background").asText());
                    card.setNotes(node.path("notes").asText());

                    // 解析外貌
                    JsonNode appearanceNode = node.path("appearance");
                    if (!appearanceNode.isMissingNode()) {
                        CharacterCardAppearance appearance = new CharacterCardAppearance();
                        appearance.setHeight(appearanceNode.path("height").asText());
                        appearance.setHair(appearanceNode.path("hair").asText());
                        appearance.setEyes(appearanceNode.path("eyes").asText());
                        appearance.setBuild(appearanceNode.path("build").asText());
                        appearance.setDistinguishingFeatures(appearanceNode.path("distinguishingFeatures").asText());
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
     * 从文本中提取JSON数组
     */
    private String extractJsonArray(String text) {
        if (text == null) return null;

        // 尝试找到JSON数组的开始和结束
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');

        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }

        return null;
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

        String systemPrompt = "根据关键词得出来的内容，我需要进行续扩写。对故事的扩写保持输入的内容的基本基调，不要偏离基调。\n" +
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
            "以下是要生成的格式的\n" +
            "[\n" +
            "  {\n" +
            "    \"section\": \"\",\n" +
            "    \"description\": \"\"\n" +
            "  }\n" +
            "].\n" +
            "\"outline\": \"xxx\"";

        String result = callDeepSeekAPI(systemPrompt, userPrompt, true, "max", 8000);
        result = cleanThinkTags(result);

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
            // 尝试解析整个JSON
            JsonNode rootNode = objectMapper.readTree(json);

            // 提取大纲
            if (rootNode.has("outline")) {
                outline.setOutline(rootNode.get("outline").asText());
            }

            // 提取章节列表
            if (rootNode.has("chapter")) {
                JsonNode chaptersNode = rootNode.get("chapter");
                List<GeneratedOutline.ChapterInfo> chapters = new ArrayList<>();
                if (chaptersNode.isArray()) {
                    for (JsonNode node : chaptersNode) {
                        GeneratedOutline.ChapterInfo chapter = new GeneratedOutline.ChapterInfo();
                        chapter.setSection(node.path("section").asText());
                        chapter.setDescription(node.path("description").asText());
                        chapters.add(chapter);
                    }
                }
                outline.setChapters(chapters);
            }
        } catch (Exception e) {
            logger.error("解析大纲JSON失败: {}", e.getMessage());
            throw new RuntimeException("解析大纲失败: " + e.getMessage());
        }

        return outline;
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

        String systemPrompt = "生成这个大纲和标题所代表的小说名字。只需要说出标题即可。标题可以带有隐喻和概括。生成的小说标题不要带有任何标点符号。叙述视角：" + pointOfView;

        String result = callDeepSeekAPI(systemPrompt, outline, false, null, 100);
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

        String systemPrompt = "你是一个人物角色总结大师。我下面给出了人物的角色卡、世界观和故事的章节概括。帮我基于章节概括里面出现的人物和世界观，只抽取在本章节内需要体现的特质。我的目的是缩小人物和世界观的所占的上下文，然后结合章节概括，提供给另一个AI来扩展生成符合人物和世界观的小说。\n\n" +
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

        String result = callDeepSeekAPI(systemPrompt, userPrompt, true, "high", 2000);
        result = cleanThinkTags(result);
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
     * @return 扩写后的章节内容
     */
    public String expandChapter(GeneratedOutline.ChapterInfo chapterInfo, String outline,
                                String relevantContext, String tools, String gameplay,
                                String genre, String requires, String pointOfView) throws Exception {
        logger.info("开始扩写章节: {}", chapterInfo.getSection());

        String systemPrompt = "女王可以使用的工具：" + tools + "\n" +
            "玩法：" + gameplay + "\n" +
            requires + "\n" +
            "# 以" + genre + "的格式进行描写。\n" +
            "体裁: " + genre + "\n" +
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

        String result = callDeepSeekAPI(systemPrompt, userPrompt, true, "max", 8000);
        result = cleanThinkTags(result);
        logger.info("章节扩写完成, 长度: {}", result.length());
        return result;
    }

    // ==================== Step 8: 去除AI味润色 ====================

    /**
     * 去除AI味润色
     * @param content 原始内容
     * @return 润色后的内容
     */
    public String polishContent(String content) throws Exception {
        logger.info("开始去除AI味润色");

        String systemPrompt = "# 质量检查清单\n" +
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
            "    - **去除过度修饰的形容词**：删减"璀璨"、"瑰丽"、"绚烂"等AI常用词堆砌\n" +
            "    - **减少抽象陈述**：把"心中涌起复杂的情感"改为具体动作/对话\n" +
            "    - **打破四字格律**：避免"心潮澎湃、热血沸腾"等陈词滥调\n" +
            "    - **增加口语化表达**：人物对话要有个性，避免"书面语套话"\n" +
            "    - **优化节奏感**：长句和短句交替，避免句式单调\n" +
            "    - **细节具象化**：用具体的视觉/听觉/嗅觉细节替代笼统描述\n" +
            "    - **去掉"记住"**： 去掉类似"记住xxx"这样的对话，替换成更贴近人的话语\n" +
            "    - **人物对话不要有陈述句**\n\n" +
            "字数不得少于4000字。";

        String result = callDeepSeekAPI(systemPrompt, content, true, "high", 8000);
        result = cleanThinkTags(result);
        logger.info("润色完成, 长度: {}", result.length());
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

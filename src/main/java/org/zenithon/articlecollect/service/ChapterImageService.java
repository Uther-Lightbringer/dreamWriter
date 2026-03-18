package org.zenithon.articlecollect.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.zenithon.articlecollect.config.EvoLinkConfig;
import org.zenithon.articlecollect.dto.CharacterCard;
import org.zenithon.articlecollect.entity.Chapter;
import org.zenithon.articlecollect.entity.CharacterCardEntity;
import org.zenithon.articlecollect.repository.ChapterRepository;
import org.zenithon.articlecollect.repository.CharacterCardRepository;
import org.zenithon.articlecollect.repository.NovelRepository;

import java.util.*;

/**
 * 章节自动配图服务
 */
@Service
public class ChapterImageService {
    
    private static final Logger logger = LoggerFactory.getLogger(ChapterImageService.class);
    
    @Autowired
    private ChapterRepository chapterRepository;
    
    @Autowired
    private AIPromptService aiPromptService;
    
    @Autowired
    private ImageService imageService;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private EvoLinkConfig evoLinkConfig;
    
    @Autowired
    private NovelRepository novelRepository;
    
    @Autowired
    private CharacterCardRepository characterCardRepository;
    
    // 每章节最大配图数量
    private static final int MAX_IMAGES_PER_CHAPTER = 4;
    
    /**
     * 分析章节内容，找出适合配图的位置（限制最多 4 个）
     * @param chapterId 章节 ID
     * @return 配图位置列表（最多 4 个）
     */
    public List<Map<String, Object>> analyzeChapterForImages(Long chapterId) {
        try {
            Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("章节不存在"));
            
            String content = chapter.getContent();
            if (content == null || content.trim().isEmpty()) {
                return new ArrayList<>();
            }
            
            // 获取角色卡信息
            List<CharacterCardEntity> characterCards = getCharacterCardsByNovelId(chapter.getNovelId());
            
            // 调用 DeepSeek 分析内容，找出适合配图的位置
            String analysisPrompt = buildAnalysisPromptWithCharacters(content, characterCards);
            String analysisResult = aiPromptService.callDeepSeekAPI(analysisPrompt);
            
            if (analysisResult == null || analysisResult.trim().isEmpty()) {
                logger.warn("DeepSeek 分析返回空结果");
                return new ArrayList<>();
            }
            
            // 解析分析结果，提取配图位置信息
            List<Map<String, Object>> imagePositions = parseImagePositions(analysisResult, content);
            
            // 不再限制数量，由用户自行选择
            logger.info("分析完成，找到 {} 个适合配图的位置", imagePositions.size());
            return imagePositions;
            
        } catch (Exception e) {
            logger.error("分析章节配图位置失败：" + e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 根据小说 ID 获取角色卡列表
     */
    private List<CharacterCardEntity> getCharacterCardsByNovelId(Long novelId) {
        try {
            // 检查小说是否存在
            if (!novelRepository.existsById(novelId)) {
                logger.warn("小说不存在：{}", novelId);
                return new ArrayList<>();
            }
            
            // 从数据库获取角色卡
            List<CharacterCardEntity> cards = characterCardRepository.findByNovelIdOrderBySortOrderAsc(novelId);
            logger.info("获取到 {} 个角色卡", cards != null ? cards.size() : 0);
            
            return cards != null ? cards : new ArrayList<>();
        } catch (Exception e) {
            logger.error("获取角色卡失败：" + e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 构建分析章节内容的 Prompt
     */
    private String buildAnalysisPrompt(String content) {
        return "你是一位专业的小说编辑，擅长为小说章节配图。请分析以下章节内容，找出所有适合用插图来增强阅读体验的位置。\n\n" +
               "要求：\n" +
               "1. 找出所有重要的场景转换、关键情节、重要人物出场等适合配图的位置\n" +
               "2. 为每个位置提供：\n" +
               "   - position: 该位置附近的原文片段（50-100 字），用于精确定位\n" +
               "   - reason: 为什么这里需要配图（如：重要场景、关键情节、人物出场等）\n" +
               "   - description: 描述这个位置适合画什么样的画面（100-200 字）\n" +
               "3. 输出格式必须为严格的 JSON 数组，格式如下：\n" +
               "[\n" +
               "  {\n" +
               "    \"position\": \"原文片段\",\n" +
               "    \"reason\": \"配图理由\",\n" +
               "    \"description\": \"画面描述\"\n" +
               "  }\n" +
               "]\n" +
               "4. 不要有任何额外的说明文字，只输出 JSON 数组\n" +
               "5. 如果没有合适配图的位置，返回空数组 []\n\n" +
               "章节内容：\n" +
               content;
    }
    
    /**
     * 构建分析章节内容的 Prompt（包含角色信息）
     */
    private String buildAnalysisPromptWithCharacters(String content, List<CharacterCardEntity> characterCards) {
        StringBuilder promptBuilder = new StringBuilder();
        
        promptBuilder.append("你是一位专业的小说编辑，擅长为小说章节配图。请分析以下章节内容，找出所有适合用插图来增强阅读体验的位置。\n\n");
        
        // 如果有角色卡，添加角色信息
        if (characterCards != null && !characterCards.isEmpty()) {
            promptBuilder.append("📚【重要角色信息】（在分析配图时需要考虑这些角色的外貌特征）：\n");
            for (int i = 0; i < characterCards.size(); i++) {
                CharacterCardEntity card = characterCards.get(i);
                promptBuilder.append("\n角色 ").append(i + 1).append(":").append(card.getName()).append("\n");
                
                // 年龄
                if (card.getAge() != null) {
                    promptBuilder.append("  - 年龄：").append(card.getAge()).append("\n");
                }
                
                // 性别
                if (card.getGender() != null && !card.getGender().isEmpty()) {
                    promptBuilder.append("  - 性别：").append(card.getGender()).append("\n");
                }
                
                // 外貌特征
                if (card.getAppearanceDescription() != null && !card.getAppearanceDescription().isEmpty()) {
                    promptBuilder.append("  - 外貌描述：").append(card.getAppearanceDescription()).append("\n");
                }
                
                // 性格特点
                if (card.getPersonality() != null && !card.getPersonality().isEmpty()) {
                    promptBuilder.append("  - 性格：").append(card.getPersonality()).append("\n");
                }
            }
            promptBuilder.append("\n");
        }
        
        promptBuilder.append("\n要求：\n");
        promptBuilder.append("1. 找出所有重要的场景转换、关键情节、重要人物出场等适合配图的位置\n");
        promptBuilder.append("2. 如果配图中包含上述角色，请根据角色的外貌特征进行画面描述\n");
        promptBuilder.append("3. 为每个位置提供：\n");
        promptBuilder.append("   - position: 该位置附近的原文片段（50-100 字），用于精确定位\n");
        promptBuilder.append("   - reason: 为什么这里需要配图（如：重要场景、关键情节、人物出场等）\n");
        promptBuilder.append("   - description: 描述这个位置适合画什么样的画面（100-200 字）。如果出现角色，必须结合角色的外貌特征进行详细描述\n");
        promptBuilder.append("4. 输出格式必须为严格的 JSON 数组，格式如下：\n");
        promptBuilder.append("[\n");
        promptBuilder.append("  {\n");
        promptBuilder.append("    \"position\": \"原文片段\",\n");
        promptBuilder.append("    \"reason\": \"配图理由\",\n");
        promptBuilder.append("    \"description\": \"画面描述\"\n");
        promptBuilder.append("  }\n");
        promptBuilder.append("]\n");
        promptBuilder.append("5. 不要有任何额外的说明文字，只输出 JSON 数组\n");
        promptBuilder.append("6. 如果没有合适配图的位置，返回空数组 []\n\n");
        promptBuilder.append("章节内容：\n");
        promptBuilder.append(content);
        
        return promptBuilder.toString();
    }
    
    /**
     * 解析 DeepSeek 返回的配图位置信息
     */
    private List<Map<String, Object>> parseImagePositions(String analysisResult, String content) {
        List<Map<String, Object>> positions = new ArrayList<>();
        
        try {
            // 尝试从文本中提取 JSON 数组
            String jsonText = extractJsonArray(analysisResult);
            if (jsonText == null || jsonText.trim().isEmpty()) {
                logger.warn("未能从分析结果中提取到 JSON 数据");
                return positions;
            }
            
            JsonNode rootNode = objectMapper.readTree(jsonText);
            
            if (!rootNode.isArray()) {
                logger.warn("分析结果不是 JSON 数组格式");
                return positions;
            }
            
            for (JsonNode node : rootNode) {
                Map<String, Object> position = new HashMap<>();
                
                String positionText = node.path("position").asText("");
                String reason = node.path("reason").asText("");
                String description = node.path("description").asText("");
                
                if (positionText.isEmpty() || description.isEmpty()) {
                    continue;
                }
                
                // 在原文中查找位置
                int index = findPositionInContent(content, positionText);
                
                position.put("position", positionText);
                position.put("reason", reason);
                position.put("description", description);
                position.put("insertIndex", index);
                
                positions.add(position);
            }
            
        } catch (Exception e) {
            logger.error("解析配图位置失败：" + e.getMessage(), e);
        }
        
        return positions;
    }
    
    /**
     * 从文本中提取 JSON 数组
     */
    private String extractJsonArray(String text) {
        // 查找第一个 [ 和最后一个 ]
        int startIndex = text.indexOf('[');
        int endIndex = text.lastIndexOf(']');
        
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return text.substring(startIndex, endIndex + 1);
        }
        
        return null;
    }
    
    /**
     * 在内容中查找位置文本
     * @return 插入位置的索引，如果未找到则返回 -1
     */
    private int findPositionInContent(String content, String positionText) {
        // 直接查找
        int index = content.indexOf(positionText);
        if (index != -1) {
            // 返回这段文本的中间位置
            return index + positionText.length() / 2;
        }
        
        // 如果找不到，尝试模糊匹配（取前 50 个字）
        String shortText = positionText.length() > 50 ? 
            positionText.substring(0, 50) : positionText;
        index = content.indexOf(shortText);
        if (index != -1) {
            return index + shortText.length() / 2;
        }
        
        // 还是找不到，返回开头
        return 0;
    }
    
    /**
     * 为配图位置生成 AI 绘画提示词（包含角色信息）- 高级提示词版本（英文输出）
     */
    public String generateImagePromptWithCharacters(String description, List<CharacterCardEntity> characterCards) {
        try {
            StringBuilder promptBuilder = new StringBuilder();

            promptBuilder.append("You are a professional AI image prompt engineer. Please generate a high-quality, detailed prompt for AI image generation based on the following scene description.\n\n");

            // 如果有角色卡，添加角色信息
            if (characterCards != null && !characterCards.isEmpty()) {
                promptBuilder.append("🎭【Important Character Appearance Information】(If the scene includes these characters, you MUST strictly follow their appearance descriptions):\n");
                for (int i = 0; i < characterCards.size(); i++) {
                    CharacterCardEntity card = characterCards.get(i);
                    promptBuilder.append("\nCharacter ").append(i + 1).append(": ").append(card.getName()).append("\n");

                    if (card.getAge() != null) {
                        promptBuilder.append("  - Age: ").append(card.getAge()).append("\n");
                    }
                    if (card.getGender() != null && !card.getGender().isEmpty()) {
                        promptBuilder.append("  - Gender: ").append(card.getGender()).append("\n");
                    }
                    if (card.getAppearanceDescription() != null && !card.getAppearanceDescription().isEmpty()) {
                        promptBuilder.append("  - Appearance: ").append(card.getAppearanceDescription()).append("\n");
                    }
                }
                promptBuilder.append("\n");
            }

            promptBuilder.append("【Advanced Prompt Structure Requirements】\n");
            promptBuilder.append("Please generate the prompt following this structure:\n");
            promptBuilder.append("1. **Basic Structure**: [Subject] + [Action/State] + [Background/Environment]\n");
            promptBuilder.append("2. **Style Modifiers**: e.g., \"cyberpunk style\", \"Studio Ghibli style\", \"Makoto Shinkai style\", \"Hayao Miyazaki style\", \"Van Gogh style\", \"Michelangelo style\", \"realism\", \"anime illustration style\", etc.\n");
            promptBuilder.append("3. **Specific Details**: Include information about composition, perspective, colors, lighting, textures, materials, etc.\n");
            promptBuilder.append("4. **Camera Angles**: e.g., \"wide-angle lens shot\", \"bird's eye view\", \"medium shot\", \"close-up shot\", \"rule of thirds composition\", \"fisheye lens\", etc.\n");
            promptBuilder.append("5. **Emotional Tone**: Describe the mood or atmosphere of the scene, e.g., \"mysterious\", \"warm and cozy\", \"epic\", \"melancholic\", \"cheerful\", \"tense\", etc.\n");
            promptBuilder.append("6. **Artist References**: e.g., \"in the style of Hayao Miyazaki\", \"in the style of Makoto Shinkai\", \"in the style of Van Gogh\", etc.\n");
            promptBuilder.append("7. **Lighting Description**: e.g., \"soft morning light\", \"cyberpunk neon lights\", \"cinematic lighting\", \"dramatic lighting\", \"sunset glow\", \"moonlight\", etc.\n");
            promptBuilder.append("8. **Texture/Material**: e.g., \"smooth marble texture\", \"polished metal surface\", \"fabric texture\", \"distressed wood surface\", etc.\n");
            promptBuilder.append("9. **Image Quality**: e.g., \"high quality, ultra HD, 8k resolution, fine details, HDR, cinematic quality\", etc.\n\n");

            promptBuilder.append("【Output Format Requirements】\n");
            promptBuilder.append("- **USE ENGLISH ONLY**\n");
            promptBuilder.append("- Only output the prompt text, no other explanations\n");
            promptBuilder.append("- Length between 200-500 words, **NEVER EXCEED 1800 CHARACTERS**\n");
            promptBuilder.append("- **MAX 1800 CHARACTERS - THIS IS CRITICAL!**\n");
            promptBuilder.append("- If the scene includes the above characters, you MUST strictly follow their appearance features (age, gender, hairstyle, eye color, clothing, etc.)\n");
            promptBuilder.append("- Avoid inappropriate content, use synonyms when necessary\n\n");

            promptBuilder.append("【Prompt Optimization Example】\n");
            promptBuilder.append("Original idea: \"an eagle\"\n");
            promptBuilder.append("Optimized prompt: \"A fierce eagle, in vibrant Japanese anime style, blending Studio Ghibli's detailed backgrounds with dynamic shonen manga action scenes. The eagle has exaggerated, expressive eyes with a determined gaze, feathers rendered with sharp, dynamic lines suggesting motion. Its wings are spread wide, massive wingspan filling the frame. The eagle wears a small samurai-inspired piece of armor on its chest, adding a touch of fantasy. Background blends traditional Japanese elements like cherry blossoms and Mount Fuji with a futuristic Tokyo skyline contrast. Scene features bright, saturated colors, dramatic lighting effects and speed lines emphasizing the eagle's power and agility. Overall composition creates energy and movement, typical of action manga scene style.\"\n\n");

            promptBuilder.append("Scene Description:\n");
            promptBuilder.append(description);

            String prompt = promptBuilder.toString();
            String result = aiPromptService.callDeepSeekAPI(prompt);
            // 限制长度在1800字符以下
            return trimPromptToLength(result, 1800);

        } catch (Exception e) {
            logger.error("生成图片提示词失败：" + e.getMessage(), e);
            return null;
        }
    }

    /**
     * 为配图位置生成 AI 绘画提示词（不含角色信息）- 高级提示词版本（英文输出）
     */
    public String generateImagePrompt(String description) {
        try {
            String prompt = "You are a professional AI image prompt engineer. Please generate a high-quality, detailed prompt for AI image generation based on the following scene description.\n\n" +

                           "【Advanced Prompt Structure Requirements】\n" +
                           "Please generate the prompt following this structure:\n" +
                           "1. **Basic Structure**: [Subject] + [Action/State] + [Background/Environment]\n" +
                           "2. **Style Modifiers**: e.g., \"cyberpunk style\", \"Studio Ghibli style\", \"Makoto Shinkai style\", \"Hayao Miyazaki style\", \"Van Gogh style\", \"Michelangelo style\", \"realism\", \"anime illustration style\", etc.\n" +
                           "3. **Specific Details**: Include information about composition, perspective, colors, lighting, textures, materials, etc.\n" +
                           "4. **Camera Angles**: e.g., \"wide-angle lens shot\", \"bird's eye view\", \"medium shot\", \"close-up shot\", \"rule of thirds composition\", \"fisheye lens\", etc.\n" +
                           "5. **Emotional Tone**: Describe the mood or atmosphere of the scene, e.g., \"mysterious\", \"warm and cozy\", \"epic\", \"melancholic\", \"cheerful\", \"tense\", etc.\n" +
                           "6. **Artist References**: e.g., \"in the style of Hayao Miyazaki\", \"in the style of Makoto Shinkai\", \"in the style of Van Gogh\", etc.\n" +
                           "7. **Lighting Description**: e.g., \"soft morning light\", \"cyberpunk neon lights\", \"cinematic lighting\", \"dramatic lighting\", \"sunset glow\", \"moonlight\", etc.\n" +
                           "8. **Texture/Material**: e.g., \"smooth marble texture\", \"polished metal surface\", \"fabric texture\", \"distressed wood surface\", etc.\n" +
                           "9. **Image Quality**: e.g., \"high quality, ultra HD, 8k resolution, fine details, HDR, cinematic quality\", etc.\n\n" +

                           "【Output Format Requirements】\n" +
                           "- **USE ENGLISH ONLY**\n" +
                           "- Only output the prompt text, no other explanations\n" +
                           "- Length between 200-500 words, **NEVER EXCEED 1800 CHARACTERS**\n" +
                           "- **MAX 1800 CHARACTERS - THIS IS CRITICAL!**\n" +
                           "- Avoid inappropriate content, use synonyms when necessary\n\n" +

                           "【Prompt Optimization Example】\n" +
                           "Original idea: \"an eagle\"\n" +
                           "Optimized prompt: \"A fierce eagle, in vibrant Japanese anime style, blending Studio Ghibli's detailed backgrounds with dynamic shonen manga action scenes. The eagle has exaggerated, expressive eyes with a determined gaze, feathers rendered with sharp, dynamic lines suggesting motion. Its wings are spread wide, massive wingspan filling the frame. The eagle wears a small samurai-inspired piece of armor on its chest, adding a touch of fantasy. Background blends traditional Japanese elements like cherry blossoms and Mount Fuji with a futuristic Tokyo skyline contrast. Scene features bright, saturated colors, dramatic lighting effects and speed lines emphasizing the eagle's power and agility. Overall composition creates energy and movement, typical of action manga scene style.\"\n\n" +

                           "Scene Description:\n" +
                           description;

            String result = aiPromptService.callDeepSeekAPI(prompt);
            // 限制长度在1800字符以下
            return trimPromptToLength(result, 1800);

        } catch (Exception e) {
            logger.error("生成图片提示词失败：" + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 执行完整的自动配图流程（包含角色卡信息，支持用户选择位置）
     * @param chapterId 章节 ID
     * @param selectedPositions 用户选择的位置索引列表（如果为 null，则生成所有位置）
     * @return 生成的图片信息列表
     */
    public List<Map<String, Object>> autoGenerateImagesForChapter(Long chapterId, List<Integer> selectedPositions) {
        List<Map<String, Object>> generatedImages = new ArrayList<>();
        
        try {
            // 1. 分析章节，找出配图位置
            List<Map<String, Object>> positions = analyzeChapterForImages(chapterId);
            logger.info("开始生成自动配的图片……");
            if (positions.isEmpty()) {
                logger.info("章节 {} 没有合适的配图位置", chapterId);
                return generatedImages;
            }
            
            // 2. 如果用户选择了位置，则过滤出选中的位置
            List<Map<String, Object>> positionsToGenerate = positions;
            if (selectedPositions != null && !selectedPositions.isEmpty()) {
                positionsToGenerate = new ArrayList<>();
                for (Integer index : selectedPositions) {
                    if (index >= 0 && index < positions.size()) {
                        positionsToGenerate.add(positions.get(index));
                    }
                }
                logger.info("用户选择了 {} 个位置进行生成", positionsToGenerate.size());
            }
            
            // 获取角色卡信息
            Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new RuntimeException("章节不存在"));
            Long novelId = chapter.getNovelId();
            List<CharacterCardEntity> characterCards = getCharacterCardsByNovelId(novelId);
            
            // 3. 为每个选中的位置生成图片
            for (Map<String, Object> position : positionsToGenerate) {
                try {
                    logger.info("正在生成位置 {} 的图片……", position.get("position"));
                    String description = (String) position.get("description");
                    
                    // 生成提示词（使用角色卡信息）
                    String prompt = generateImagePromptWithCharacters(description, characterCards);
                    if (prompt == null || prompt.trim().isEmpty()) {
                        logger.warn("提示词生成失败，跳过此位置");
                        continue;
                    }
                    logger.info("自动配图的提示词：" + prompt);
                    // 调用图片生成服务
                    String imageUrl = generateAndSaveImage(chapterId, prompt);
                    logger.info("自动配图成功，图片 URL：" + imageUrl);
                    if (imageUrl != null) {
                        Map<String, Object> imageInfo = new HashMap<>();
                        imageInfo.put("position", position.get("position"));
                        imageInfo.put("insertIndex", position.get("insertIndex"));
                        imageInfo.put("imageUrl", imageUrl);
                        imageInfo.put("prompt", prompt);
                        
                        generatedImages.add(imageInfo);
                    }
                    
                } catch (Exception e) {
                    logger.error("生成单个图片失败：" + e.getMessage(), e);
                    // 继续处理下一个
                }
            }
            
        } catch (Exception e) {
            logger.error("自动配图流程失败：" + e.getMessage(), e);
        }
        
        return generatedImages;
    }
    
    /**
     * 执行完整的自动配图流程（旧版本，生成所有位置）
     */
    public List<Map<String, Object>> autoGenerateImagesForChapter(Long chapterId) {
        return autoGenerateImagesForChapter(chapterId, null);
    }
    
    /**
     * 生成并保存图片到本地
     */
    private String generateAndSaveImage(Long chapterId, String prompt) {
        try {
            // 创建 EvoLinkImageService 实例
            EvoLinkImageService imageService = new EvoLinkImageService(evoLinkConfig, restTemplate, objectMapper);
            String taskId = imageService.generateImage(prompt, "16:9");
            
            // 等待图片生成完成
            String imageUrl = waitForImageCompletion(imageService, taskId, 5 * 60 * 1000);
            
            if (imageUrl != null) {
                // 下载并保存到本地
                String localPath = downloadAndSaveToLocal(chapterId, imageUrl);
                return localPath;
            }
            
            return null;
            
        } catch (Exception e) {
            logger.error("生成并保存图片失败：" + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 下载图片并保存到本地章节目录
     */
    private String downloadAndSaveToLocal(Long chapterId, String imageUrl) {
        try {
            // 下载图片
            ResponseEntity<byte[]> response = restTemplate.getForEntity(imageUrl, byte[].class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // 获取章节信息用于命名
                Chapter chapter = chapterRepository.findById(chapterId)
                    .orElseThrow(() -> new RuntimeException("章节不存在"));
                
                // 使用 FileUploadUtil 保存图片
                String localPath = org.zenithon.articlecollect.util.FileUploadUtil.saveAutoGeneratedImage(
                    response.getBody(),
                    chapterId,
                    chapter.getTitle()
                );
                
                logger.info("图片已保存到本地：{}", localPath);
                return localPath;
            }
            
            return null;
            
        } catch (Exception e) {
            logger.error("下载并保存图片失败：" + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 等待图片生成完成
     */
    private String waitForImageCompletion(EvoLinkImageService imageService, String taskId, long timeout) {
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < timeout) {
            try {
                EvoLinkImageService.TaskStatus status = imageService.getTaskStatus(taskId);
                
                if (status.isCompleted()) {
                    return status.getImageUrl();
                }
                
                if (status.isFailed()) {
                    logger.error("图片生成失败：{}", status.getError());
                    return null;
                }
                
                // 等待 2 秒后再次检查
                Thread.sleep(2000);
                
            } catch (Exception e) {
                logger.error("检查任务状态失败：" + e.getMessage(), e);
                return null;
            }
        }
        
        logger.error("等待图片生成超时");
        return null;
    }
    
    /**
     * 将图片插入到章节内容中
     * @param content 原章节内容
     * @param images 图片信息列表
     * @return 插入图片后的新内容
     */
    public String insertImagesToContent(String content, List<Map<String, Object>> images) {
        if (images.isEmpty()) {
            return content;
        }
        
        // 按插入位置倒序排序，避免索引变化
        List<Map<String, Object>> sortedImages = new ArrayList<>(images);
        sortedImages.sort((a, b) -> {
            Integer indexA = (Integer) a.get("insertIndex");
            Integer indexB = (Integer) b.get("insertIndex");
            return indexB.compareTo(indexA); // 倒序
        });
        
        StringBuilder newContent = new StringBuilder(content);
        
        for (Map<String, Object> image : sortedImages) {
            Integer insertIndex = (Integer) image.get("insertIndex");
            String imageUrl = (String) image.get("imageUrl");
            
            if (insertIndex != null && imageUrl != null) {
                // 插入 Markdown 格式的图片
                String imageMarkdown = "\n\n![配图](" + imageUrl + ")\n\n";
                
                // 确保不越界
                if (insertIndex < 0) insertIndex = 0;
                if (insertIndex > newContent.length()) insertIndex = newContent.length();
                
                newContent.insert(insertIndex, imageMarkdown);
            }
        }
        
        return newContent.toString();
    }

    /**
     * 将提示词截断到指定字符数以内
     * 在最后一个空格或标点处截断，避免截断单词
     */
    private String trimPromptToLength(String prompt, int maxLength) {
        if (prompt == null || prompt.length() <= maxLength) {
            return prompt;
        }

        // 先尝试在最后一个空格处截断
        int lastSpace = prompt.lastIndexOf(' ', maxLength);
        if (lastSpace > maxLength * 0.7) { // 确保不会截断太短
            return prompt.substring(0, lastSpace);
        }

        // 尝试在标点符号处截断
        char[] punctuation = {'.', ',', '!', '?', ';', ':'};
        for (char p : punctuation) {
            int lastPunc = prompt.lastIndexOf(p, maxLength);
            if (lastPunc > maxLength * 0.7) {
                return prompt.substring(0, lastPunc + 1);
            }
        }

        // 如果没有找到合适的截断点，就直接截断
        logger.warn("提示词过长({}字符)，已截断到{}字符", prompt.length(), maxLength);
        return prompt.substring(0, maxLength);
    }
}

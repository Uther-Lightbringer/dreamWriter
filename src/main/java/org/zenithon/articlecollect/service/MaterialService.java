package org.zenithon.articlecollect.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * 素材库服务
 * 支持按场景关键词按需加载创作素材
 */
@Service
public class MaterialService {

    private static final Logger logger = LoggerFactory.getLogger(MaterialService.class);

    // 素材目录基础路径
    private static final String MATERIALS_BASE = "words/materials/";

    // 场景关键词 → 素材文件映射
    private static final Map<String, List<String>> SCENE_MAPPING = new LinkedHashMap<>();

    static {
        // 人物相关
        SCENE_MAPPING.put("外貌", List.of(
            "character/facial-features.md",
            "character/other-parts.md"
        ));
        SCENE_MAPPING.put("表情", List.of(
            "character/expression.md",
            "character/facial-features.md"
        ));
        SCENE_MAPPING.put("神态", List.of(
            "character/expression.md"
        ));
        SCENE_MAPPING.put("情绪", List.of(
            "character/emotion.md",
            "character/psychology.md"
        ));
        SCENE_MAPPING.put("心理", List.of(
            "character/psychology.md"
        ));
        SCENE_MAPPING.put("心动", List.of(
            "character/emotion-romantic.md",
            "character/body-language-romantic.md"
        ));
        SCENE_MAPPING.put("暧昧", List.of(
            "character/emotion-romantic.md",
            "character/body-language-romantic.md",
            "techniques/atmosphere-romantic.md"
        ));
        SCENE_MAPPING.put("暗恋", List.of(
            "character/emotion-romantic.md",
            "character/psychology.md"
        ));
        SCENE_MAPPING.put("甜蜜", List.of(
            "character/emotion-romantic.md",
            "character/body-language-romantic.md"
        ));
        SCENE_MAPPING.put("性格", List.of(
            "character/personality.md"
        ));
        SCENE_MAPPING.put("动作", List.of(
            "character/action.md"
        ));
        SCENE_MAPPING.put("声音", List.of(
            "character/voice.md",
            "other/sounds.md"
        ));
        SCENE_MAPPING.put("肢体", List.of(
            "character/body-language-romantic.md",
            "character/action.md"
        ));
        SCENE_MAPPING.put("牵手", List.of(
            "character/body-language-romantic.md"
        ));
        SCENE_MAPPING.put("拥抱", List.of(
            "character/body-language-romantic.md"
        ));
        SCENE_MAPPING.put("亲密", List.of(
            "character/body-language-romantic.md",
            "techniques/atmosphere-romantic.md"
        ));

        // 服饰相关
        SCENE_MAPPING.put("服饰", List.of(
            "character/clothing/general.md"
        ));
        SCENE_MAPPING.put("古代服饰", List.of(
            "character/clothing/ancient-female.md",
            "character/clothing/ancient-male.md"
        ));
        SCENE_MAPPING.put("现代服饰", List.of(
            "character/clothing/modern-female.md",
            "character/clothing/modern-male.md"
        ));
        SCENE_MAPPING.put("古装女性", List.of(
            "character/clothing/ancient-female.md"
        ));
        SCENE_MAPPING.put("古装男性", List.of(
            "character/clothing/ancient-male.md"
        ));
        SCENE_MAPPING.put("现代女性", List.of(
            "character/clothing/modern-female.md"
        ));
        SCENE_MAPPING.put("现代男性", List.of(
            "character/clothing/modern-male.md"
        ));
        SCENE_MAPPING.put("配饰", List.of(
            "character/clothing/modern-accessories.md"
        ));
        SCENE_MAPPING.put("鞋", List.of(
            "character/clothing/modern-footwear.md"
        ));
        SCENE_MAPPING.put("包", List.of(
            "character/clothing/modern-bags.md"
        ));
        SCENE_MAPPING.put("穿搭", List.of(
            "character/clothing/modern-styles.md"
        ));

        // 自然风景
        SCENE_MAPPING.put("风景", List.of(
            "nature/landscape.md",
            "nature/celestial.md"
        ));
        SCENE_MAPPING.put("天空", List.of(
            "nature/celestial.md"
        ));
        SCENE_MAPPING.put("日月", List.of(
            "nature/celestial.md"
        ));
        SCENE_MAPPING.put("山川", List.of(
            "nature/landscape.md"
        ));
        SCENE_MAPPING.put("河海", List.of(
            "nature/landscape.md"
        ));
        SCENE_MAPPING.put("天气", List.of(
            "nature/weather.md"
        ));
        SCENE_MAPPING.put("雨雪", List.of(
            "nature/weather.md"
        ));
        SCENE_MAPPING.put("季节", List.of(
            "nature/seasons.md"
        ));
        SCENE_MAPPING.put("春天", List.of(
            "nature/seasons.md"
        ));
        SCENE_MAPPING.put("夏天", List.of(
            "nature/seasons.md"
        ));
        SCENE_MAPPING.put("秋天", List.of(
            "nature/seasons.md"
        ));
        SCENE_MAPPING.put("冬天", List.of(
            "nature/seasons.md"
        ));
        SCENE_MAPPING.put("灾难", List.of(
            "nature/disaster.md"
        ));
        SCENE_MAPPING.put("末日", List.of(
            "nature/disaster.md"
        ));

        // 古代知识
        SCENE_MAPPING.put("古代", List.of(
            "knowledge/ancient-basics.md",
            "knowledge/ancient-time.md"
        ));
        SCENE_MAPPING.put("古代基础", List.of(
            "knowledge/ancient-basics.md"
        ));
        SCENE_MAPPING.put("五行", List.of(
            "knowledge/ancient-basics.md"
        ));
        SCENE_MAPPING.put("八卦", List.of(
            "knowledge/ancient-basics.md"
        ));
        SCENE_MAPPING.put("兵器", List.of(
            "knowledge/ancient-weapons.md"
        ));
        SCENE_MAPPING.put("武器", List.of(
            "knowledge/ancient-weapons.md"
        ));
        SCENE_MAPPING.put("古代时间", List.of(
            "knowledge/ancient-time.md"
        ));

        // 写作技巧
        SCENE_MAPPING.put("美人", List.of(
            "techniques/beauty-standards.md"
        ));
        SCENE_MAPPING.put("美女", List.of(
            "techniques/beauty-standards.md"
        ));
        SCENE_MAPPING.put("打斗", List.of(
            "techniques/fight-formulas.md",
            "character/action.md"
        ));
        SCENE_MAPPING.put("战斗", List.of(
            "techniques/fight-formulas.md",
            "character/action.md"
        ));
        SCENE_MAPPING.put("武功", List.of(
            "techniques/fight-formulas.md"
        ));
        SCENE_MAPPING.put("氛围", List.of(
            "techniques/atmosphere-romantic.md"
        ));
        SCENE_MAPPING.put("浪漫", List.of(
            "techniques/atmosphere-romantic.md",
            "character/emotion-romantic.md"
        ));

        // 其他
        SCENE_MAPPING.put("颜色", List.of(
            "other/colors.md"
        ));
    }

    /**
     * 根据场景关键词获取相关素材
     *
     * @param scene 场景关键词（如：外貌、服饰、风景等）
     * @return 素材内容，如果关键词不存在则返回可用关键词列表
     */
    public String getMaterialsByScene(String scene) {
        if (scene == null || scene.trim().isEmpty()) {
            return getAvailableScenes();
        }

        List<String> files = SCENE_MAPPING.get(scene.trim());
        if (files == null || files.isEmpty()) {
            // 尝试模糊匹配
            List<String> matchedScenes = findSimilarScenes(scene.trim());
            if (!matchedScenes.isEmpty()) {
                StringBuilder result = new StringBuilder();
                result.append("未找到关键词「").append(scene).append("」的精确匹配。\n");
                result.append("你是否想要查找：").append(String.join("、", matchedScenes)).append("\n\n");
                result.append("可用的场景关键词：\n").append(getAvailableScenes());
                return result.toString();
            }
            return "未找到关键词「" + scene + "」的素材。\n\n可用的场景关键词：\n" + getAvailableScenes();
        }

        StringBuilder content = new StringBuilder();
        content.append("## 素材：").append(scene).append("\n\n");

        for (String file : files) {
            String materialContent = readMaterialFile(file);
            if (materialContent != null) {
                content.append(materialContent);
                content.append("\n\n---\n\n");
            }
        }

        logger.info("加载素材: scene={}, files={}", scene, files);
        return content.toString();
    }

    /**
     * 根据场景关键词和详细描述获取素材
     *
     * @param scene  场景关键词
     * @param detail 详细描述（用于更精确的匹配）
     * @return 素材内容
     */
    public String getMaterialsByScene(String scene, String detail) {
        // 如果有详细描述，尝试组合匹配
        if (detail != null && !detail.trim().isEmpty()) {
            String combinedKey = scene + detail;
            if (SCENE_MAPPING.containsKey(combinedKey)) {
                return getMaterialsByScene(combinedKey);
            }

            // 尝试分别匹配
            StringBuilder result = new StringBuilder();
            result.append(getMaterialsByScene(scene));

            // 如果 detail 本身是有效的场景关键词
            if (SCENE_MAPPING.containsKey(detail)) {
                result.append("\n\n");
                result.append(getMaterialsByScene(detail));
            }

            return result.toString();
        }

        return getMaterialsByScene(scene);
    }

    /**
     * 获取所有可用的场景关键词
     */
    public String getAvailableScenes() {
        StringBuilder sb = new StringBuilder();
        sb.append("可用的场景关键词：\n");

        // 按类别分组显示
        sb.append("\n【人物描写】外貌、表情、神态、情绪、心理、性格、动作、声音、肢体\n");
        sb.append("【情感互动】心动、暧昧、暗恋、甜蜜、牵手、拥抱、亲密\n");
        sb.append("【服饰穿搭】服饰、古代服饰、现代服饰、古装女性、古装男性、现代女性、现代男性、配饰、鞋、包、穿搭\n");
        sb.append("【自然风景】风景、天空、日月、山川、河海、天气、雨雪、季节、春天、夏天、秋天、冬天、灾难、末日\n");
        sb.append("【古代知识】古代、古代基础、五行、八卦、兵器、武器、古代时间\n");
        sb.append("【写作技巧】美人、美女、打斗、战斗、武功、氛围、浪漫\n");
        sb.append("【其他】颜色、声音\n");

        return sb.toString();
    }

    /**
     * 获取素材目录索引（轻量级，不加载内容）
     */
    public String getMaterialIndex() {
        return """
            ## 素材库结构

            ```
            materials/
            ├── character/          # 人物描写
            │   ├── facial-features.md    # 五官描写
            │   ├── other-parts.md        # 头发、脸型
            │   ├── expression.md         # 神态描写
            │   ├── emotion.md            # 情感描写
            │   ├── emotion-romantic.md   # 暧昧情感描写
            │   ├── personality.md        # 性格描写
            │   ├── psychology.md         # 心理描写
            │   ├── action.md             # 动作描写
            │   ├── body-language-romantic.md # 肢体语言互动
            │   ├── voice.md              # 声音描写
            │   └── clothing/             # 服饰描写
            │       ├── ancient-female.md   # 古代女性服饰
            │       ├── ancient-male.md     # 古代男性服饰
            │       ├── modern-female.md    # 现代女性服饰
            │       ├── modern-male.md      # 现代男性服饰
            │       └── ...
            ├── nature/             # 自然风景
            │   ├── celestial.md          # 日月星辰
            │   ├── landscape.md          # 山川河海
            │   ├── weather.md            # 风霜雨雪
            │   └── seasons.md            # 春夏秋冬
            ├── knowledge/          # 古代知识
            │   ├── ancient-basics.md     # 古代基础
            │   ├── ancient-weapons.md    # 古代兵器
            │   └── ancient-time.md       # 古代计时法
            ├── techniques/         # 写作技巧
            │   ├── beauty-standards.md   # 美人标准
            │   ├── fight-formulas.md     # 打斗七公式
            │   └── atmosphere-romantic.md # 氛围营造技巧
            └── other/              # 其他素材
                ├── colors.md             # 中国传统颜色
                └── sounds.md             # 声音描写
            ```

            使用 get_material 工具，指定场景关键词即可获取相关素材。
            """;
    }

    /**
     * 读取单个素材文件
     *
     * @param relativePath 相对于 materials/ 目录的路径
     * @return 文件内容，读取失败返回 null
     */
    public String readMaterialFile(String relativePath) {
        try {
            ClassPathResource resource = new ClassPathResource(MATERIALS_BASE + relativePath);
            if (!resource.exists()) {
                logger.warn("素材文件不存在: {}", relativePath);
                return null;
            }
            byte[] bytes = resource.getInputStream().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("读取素材文件失败: {}", relativePath, e);
            return null;
        }
    }

    /**
     * 模糊查找相似的场景关键词
     */
    private List<String> findSimilarScenes(String input) {
        List<String> similar = new ArrayList<>();
        for (String key : SCENE_MAPPING.keySet()) {
            if (key.contains(input) || input.contains(key)) {
                similar.add(key);
            }
        }
        return similar;
    }
}

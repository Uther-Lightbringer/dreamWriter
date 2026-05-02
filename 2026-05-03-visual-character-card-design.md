# 视觉叙事角色卡系统设计

## 概述

为视觉叙事作品增加独立的角色卡体系，实现分镜图片生成时自动识别角色并保持视觉一致性。

## 一、目标

1. 视觉叙事作品拥有独立的角色卡
2. 生成分镜图片时自动识别 scene 中出现的角色
3. 角色 + 场景组合 seed 保障一致性
4. scene 中的外貌描述可覆盖角色卡基础外貌

## 二、数据模型

### 2.1 新增实体：VisualCharacterCard

```java
@Entity
@Table(name = "visual_character_cards")
public class VisualCharacterCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_id", nullable = false)
    private Long workId;                    // 关联 VisualWork

    @Column(nullable = false, length = 255)
    private String name;                    // 角色名称

    @Column(name = "alternative_names", columnDefinition = "TEXT")
    private String alternativeNames;        // 别名列表 (JSON Array)

    @Column(name = "appearance_json", columnDefinition = "TEXT")
    private String appearanceJson;          // 结构化外貌特征 (JSON)

    @Column(name = "appearance_description", columnDefinition = "TEXT")
    private String appearanceDescription;   // 外貌描述文本 (用于 AI 绘图)

    @Column(nullable = false)
    private Integer seed;                   // 角色一致性种子 (1-2147483647)

    @Column(length = 20)
    private String role;                    // 角色类型: protagonist/supporting

    @Column(columnDefinition = "TEXT")
    private String notes;                   // 备注

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
```

### 2.2 修改实体：VisualPanel

新增字段：

```java
@Column(name = "detected_character_ids", columnDefinition = "TEXT")
private String detectedCharacterIds;       // AI 检测到的角色 ID 列表 (JSON Array)

@Column(name = "scene_override", columnDefinition = "TEXT")
private String sceneOverride;              // 场景中外貌变化的描述
```

### 2.3 修改实体：VisualWork

新增字段：

```java
@Column(name = "base_seed")
private Integer baseSeed;                  // 作品基础 seed，用于场景一致性
```

## 三、核心服务

### 3.1 新增服务：VisualCharacterCardService

负责角色卡的 CRUD 操作，参考现有 CharacterCardService 实现。

核心方法：
- `getByWorkId(Long workId)` - 获取作品所有角色卡
- `save(Long workId, VisualCharacterCard card)` - 保存角色卡（自动生成 seed）
- `update(Long cardId, VisualCharacterCard card)` - 更新角色卡
- `delete(Long cardId)` - 删除角色卡
- `getById(Long cardId)` - 获取单个角色卡

### 3.2 新增服务：VisualCharacterMatchService

负责从 scene 文本中识别角色。

**核心方法**：
```java
public List<MatchedCharacter> matchCharacters(Long workId, String sceneText);
```

**匹配流程**：
1. 加载作品的所有角色卡
2. 构建角色名称列表（含别名）作为提示
3. 调用 DeepSeek API，让 AI 识别 scene 中出现的角色
4. AI 同时提取 scene 中描述的外貌变化
5. 返回匹配结果

**返回结构**：
```java
public class MatchedCharacter {
    private Long characterId;
    private String characterName;
    private Integer seed;
    private String appearanceOverride;  // scene 中的外貌变化
}
```

**AI Prompt 模板**：
```
你是一个角色识别助手。根据以下角色列表，识别文本中出现的角色。

角色列表：
1. 小明 (别名: 明明, 小明哥)
2. 小红 (别名: 红红)

请分析以下文本，返回 JSON 格式：
{
  "matchedCharacters": [
    {
      "name": "小明",
      "appearanceOverride": "戴着红色帽子"  // 如果文本中描述了外貌变化
    }
  ]
}

文本：{sceneText}

只返回 JSON，不要其他内容。
```

### 3.3 修改服务：VisualImageAsyncService

修改 `buildImagePrompt()` 方法，整合角色信息。

**新提示词结构**：
```
{globalStyle}, {panelStyle},
{角色1外貌描述}, {角色2外貌描述},
{sceneOverride},
{panel.scene}, {cameraAngle} shot, {action},
manga panel, comic art, high quality
```

**Seed 组合算法**：
```java
private int combineSeeds(int baseSeed, List<Integer> characterSeeds) {
    int combined = baseSeed;
    for (Integer seed : characterSeeds) {
        combined = combined * 31 + seed;
    }
    return Math.abs(combined % 2147483646) + 1;
}
```

### 3.4 修改服务：VisualGenreService

新增 AI 工具：

| 工具名 | 说明 | 参数 |
|--------|------|------|
| create_visual_character | 创建角色卡 | workId, name, appearance, role |
| update_visual_character | 更新角色卡 | characterId, fields... |
| list_visual_characters | 列出角色卡 | workId |
| delete_visual_character | 删除角色卡 | characterId |

修改现有工具：

`add_panel` 新增可选参数 `characters` (角色 ID 数组)
`update_panel` 新增可选参数 `characters`

## 四、数据流程

```
用户输入分镜 scene
        ↓
┌─────────────────────────────────┐
│  VisualCharacterMatchService    │
│  1. 加载角色卡列表               │
│  2. 名称匹配 + AI 识别           │
│  3. 提取外貌覆盖描述             │
└─────────────────────────────────┘
        ↓
   匹配的角色列表
        ↓
┌─────────────────────────────────┐
│  VisualImageAsyncService        │
│  1. 构建提示词                   │
│  2. 合并角色外貌 + 场景覆盖      │
│  3. 计算组合 seed                │
│  4. 调用 EvoLink 生成图片        │
└─────────────────────────────────┘
        ↓
     生成的图片
```

## 五、API 接口

### 5.1 REST API

```
GET    /api/visual-works/{workId}/characters        # 获取角色卡列表
POST   /api/visual-works/{workId}/characters        # 创建角色卡
PUT    /api/visual-works/{workId}/characters/{id}   # 更新角色卡
DELETE /api/visual-works/{workId}/characters/{id}   # 删除角色卡
```

### 5.2 请求/响应示例

**创建角色卡请求**：
```json
{
  "name": "小明",
  "alternativeNames": ["明明", "小明哥"],
  "appearance": {
    "hair": "黑色长发",
    "eyes": "深棕色眼睛",
    "build": "身材修长",
    "clothing": "白色衬衫"
  },
  "role": "protagonist"
}
```

**响应**：
```json
{
  "id": 1,
  "workId": 100,
  "name": "小明",
  "appearanceDescription": "黑色长发，深棕色眼睛，身材修长，穿着白色衬衫",
  "seed": 12345,
  "role": "protagonist"
}
```

## 六、前端改动

### 6.1 新增页面组件

- 角色卡管理模态框（类似现有小说角色卡）
- 分镜编辑时显示识别到的角色

### 6.2 修改现有页面

- `visual-detail.html` 添加角色卡入口
- 分镜卡片显示识别到的角色标签

## 七、实现优先级

1. **P0 - 核心功能**
   - VisualCharacterCard 实体
   - VisualCharacterCardService
   - VisualCharacterMatchService
   - 修改 VisualImageAsyncService

2. **P1 - AI 工具集成**
   - VisualGenreService 新增工具
   - 角色识别集成到图片生成流程

3. **P2 - 前端界面**
   - 角色卡管理界面
   - 分镜显示角色标签

# 小说管理平台

这是一个基于Spring Boot的小说管理平台，提供了完整的前后端功能。

## 功能特性

### 📚 小说管理
- 创建新小说（只需输入标题）
- 查看所有小说列表
- 删除小说（同时删除相关章节）
- 世界观设置与角色卡管理

### 📖 章节管理
- 为小说创建章节（标题 + 内容）
- 查看小说的所有章节
- 删除特定章节
- 章节标签自动分析

### 📱 阅读体验
- **章节详情页面**：单独打开章节进行全文阅读
- **智能导航**：支持上一章、下一章快速切换
- **章节列表**：随时返回小说的章节列表
- **面包屑导航**：清晰的页面路径指示

### 🎨 AI功能
- **AI配图**：自动分析章节内容，调用AI生成配图
- **AI聊天**：集成DeepSeek AI对话功能
- **角色图像生成**：为角色卡自动生成AI图像

### 🔧 REST API接口
- 完整的HTTP API供外部调用
- 新增章节详情查询接口

## 技术栈

- **后端**: Spring Boot 3.0.2 + Java 17
- **前端**: HTML + JavaScript + CSS + Thymeleaf
- **数据存储**: H2文件数据库
- **构建工具**: Maven

## 🚀 使用流程

1. **创建小说**：在首页输入小说标题创建新小说
2. **管理章节**：点击"管理章节"进入小说详情页
3. **添加章节**：在小说详情页添加新章节
4. **阅读章节**：点击"阅读"按钮进入章节详情页
5. **导航浏览**：使用上一章/下一章按钮连续阅读

## 修改数据库
启动您的应用
在浏览器中访问：http://localhost:38081/h2-console
连接信息：
JDBC URL: jdbc:h2:file:./data/article_collect
User Name: sa
Password: （留空）


## API接口文档

### 小说相关接口

#### 1. 创建小说
```
POST /api/novels
Content-Type: application/json

{
    "title": "小说标题"
}

返回:
{
    "id": 1,
    "title": "小说标题",
    "createTime": "2024-01-01T10:00:00",
    "updateTime": "2024-01-01T10:00:00"
}
```

#### 2. 获取所有小说
```
GET /api/novels

返回:
[
    {
        "id": 1,
        "title": "小说标题",
        "createTime": "2024-01-01T10:00:00",
        "updateTime": "2024-01-01T10:00:00"
    }
]
```

#### 3. 获取特定小说
```
GET /api/novels/{novelId}
```

#### 4. 删除小说
```
DELETE /api/novels/{novelId}

返回:
{
    "success": true
}
```

### 章节相关接口

#### 1. 为小说创建章节
```
POST /api/novels/{novelId}/chapters
Content-Type: application/json

{
    "title": "章节标题",
    "content": "章节内容"
}

返回:
{
    "id": 1,
    "novelId": 1,
    "title": "章节标题",
    "content": "章节内容",
    "chapterNumber": 1,
    "createTime": "2024-01-01T10:00:00",
    "updateTime": "2024-01-01T10:00:00"
}
```

#### 2. 获取小说的所有章节
```
GET /api/novels/{novelId}/chapters

返回:
[
    {
        "id": 1,
        "novelId": 1,
        "title": "第一章",
        "content": "章节内容",
        "chapterNumber": 1,
        "createTime": "2024-01-01T10:00:00",
        "updateTime": "2024-01-01T10:00:00"
    }
]
```

#### 3. 获取特定章节
```
GET /api/novels/{novelId}/chapters/{chapterId}
```

#### 4. 获取章节详情（新增）
```
GET /api/chapters/detail/{novelId}/{chapterId}

返回:
{
    "chapter": {
        "id": 1,
        "novelId": 1,
        "title": "章节标题",
        "content": "章节完整内容",
        "chapterNumber": 1,
        "createTime": "2024-01-01T10:00:00",
        "updateTime": "2024-01-01T10:00:00"
    },
    "hasNext": true,
    "hasPrevious": false,
    "nextChapterId": 2,
    "previousChapterId": null,
    "novelTitle": "小说标题",
    "novelId": 1
}
```

#### 5. 更新章节
```
PUT /api/novels/{novelId}/chapters/{chapterId}
Content-Type: application/json

{
    "title": "新标题",
    "content": "新内容"
}
```

#### 6. 删除章节
```
DELETE /api/novels/{novelId}/chapters/{chapterId}

返回:
{
    "success": true
}
```

## 如何运行

### 方式一：使用IDEA运行
1. 在IntelliJ IDEA中打开项目
2. 确保已安装Java 17
3. 运行 `ArticleCollectApplication.java` 中的main方法

### 方式二：使用Maven命令行
```bash
# 编译项目
mvn clean compile

# 运行项目
mvn spring-boot:run

# 或者打包后运行
mvn clean package
java -jar target/articleCollect-0.0.1-SNAPSHOT.jar
```

## 页面导航结构

```
首页 (/) 
├── 创建小说
├── 小说列表
│   └── 点击"管理章节" → 小说详情页 (/novel/{novelId})
│       ├── 添加新章节
│       ├── 章节列表
│       │   └── 点击"阅读" → 章节详情页 (/novel/{novelId}/chapter/{chapterId})
│       │       ├── 章节全文阅读
│       │       ├── 上一章/下一章导航
│       │       └── 返回章节列表
│       └── 删除小说
└── 删除小说
```

## 项目结构

```
src/
├── main/
│   ├── java/
│   │   └── org/zenithon/articlecollect/
│   │       ├── controller/          # Web和REST API控制器
│   │       ├── service/             # 业务逻辑实现
│   │       ├── repository/          # 数据访问层
│   │       ├── entity/              # JPA实体类
│   │       ├── dto/                 # 数据传输对象
│   │       ├── config/              # 配置类
│   │       ├── util/                # 工具类
│   │       └── ArticleCollectApplication.java
│   └── resources/
│       ├── templates/               # Thymeleaf模板
│       ├── static/                  # 静态资源
│       └── application.properties   # 配置文件
└── test/
```

## 注意事项

- 数据存储在H2文件数据库中，应用重启后数据保留
- 默认端口为38081，可在application.properties中修改
- AI功能需要配置相应的API密钥环境变量

## 环境变量配置

| 环境变量 | 说明 |
|---------|------|
| `DEEPSEEK_API_KEY` | DeepSeek AI聊天API密钥 |
| `EVOLINK_API_TOKEN` | EvoLink图片生成API令牌 |
| `VOLCENGINE_ACCESS_KEY` | 火山引擎Access Key |
| `VOLCENGINE_SECRET_KEY` | 火山引擎Secret Key |
-- 角色卡数据库表创建脚本
-- 执行时间：2026-03-15

-- 创建 character_cards 表（兼容 H2 和 MySQL）
CREATE TABLE IF NOT EXISTS character_cards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,                    -- 数据库主键 ID
    character_id VARCHAR(100) NOT NULL,                      -- 角色唯一标识（如：char-001）
    name VARCHAR(255) NOT NULL,                              -- 角色姓名
    alternative_names VARCHAR(1000),                         -- 别名（JSON 数组格式）
    age INT,                                                  -- 年龄
    gender VARCHAR(50),                                       -- 性别
    occupation VARCHAR(255),                                  -- 职业
    appearance_json TEXT,                                     -- 外貌特征（JSON 格式）
    personality TEXT,                                         -- 性格描述
    background TEXT,                                          -- 背景故事
    relationships_json TEXT,                                  -- 人际关系（JSON 格式）
    notes TEXT,                                               -- 备注
    novel_id BIGINT NOT NULL,                                 -- 所属小说 ID（外键）
    sort_order INT,                                           -- 排序顺序
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,          -- 创建时间
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP -- 更新时间
);

-- 说明：
-- 1. 每个角色占用一行数据
-- 2. novel_id 关联到 novels 表的主键
-- 3. appearance_json 和 relationships_json 字段存储 JSON 格式的复杂对象
-- 4. sort_order 用于控制角色卡的显示顺序
-- 5. 本脚本已针对 H2 数据库优化，移除了 INDEX 语法和外键约束
--    JPA/Hibernate 会在启动时自动管理表关系

-- 如果需要手动添加索引，可以使用以下 SQL（可选）：
-- CREATE INDEX idx_novel_id ON character_cards(novel_id);
-- CREATE INDEX idx_character_id ON character_cards(character_id);

-- 示例数据：
-- INSERT INTO character_cards (character_id, name, age, gender, occupation, novel_id, sort_order) 
-- VALUES ('char-001', '张三', 18, '男', '学生', 1, 0);

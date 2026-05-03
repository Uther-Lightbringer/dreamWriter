-- 为 character_cards 表添加版本号字段
-- 执行时间：2026-03-15

-- 添加提示词版本号字段
ALTER TABLE character_cards ADD COLUMN prompt_version INTEGER DEFAULT 0;

-- 添加图片版本号字段
ALTER TABLE character_cards ADD COLUMN image_version INTEGER DEFAULT 0;

-- 更新现有数据（如果有）
UPDATE character_cards SET prompt_version = 0 WHERE prompt_version IS NULL;
UPDATE character_cards SET image_version = 0 WHERE image_version IS NULL;

-- 添加注释（如果数据库支持）
COMMENT ON COLUMN character_cards.prompt_version IS 'AI 绘画提示词版本号，每次重新生成递增';
COMMENT ON COLUMN character_cards.image_version IS '生成的图片版本号，每次重新生成递增';

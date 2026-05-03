-- 删除 character_cards 表中的 CHARACTER_ID 字段
-- 执行时间：2026-03-15
-- 说明：统一使用数据库自增 ID 作为唯一标识

-- 1. 备份数据（可选，建议先备份）
-- CREATE TABLE character_cards_backup AS SELECT * FROM character_cards;

-- 2. 删除 CHARACTER_ID 字段
ALTER TABLE character_cards DROP COLUMN IF EXISTS CHARACTER_ID;

-- 3. 验证字段已删除
-- SELECT * FROM character_cards LIMIT 1;

-- 4. 查看表结构确认
-- DESCRIBE character_cards;

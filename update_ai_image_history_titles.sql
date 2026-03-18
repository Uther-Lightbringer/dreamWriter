-- ====================================
-- 修复 AI 图片历史记录的小说名称显示
-- ====================================
-- 用途：为已有的历史记录填充 novel_title 和 chapter_title 字段
-- 说明：通过 novel_id 和 chapter_id 关联查询，从小说表和章节表中获取标题信息
-- ====================================

-- 1. 更新小说标题（通过 novel_id 关联）
UPDATE ai_image_history h
SET novel_title = (
    SELECT n.title 
    FROM novels n 
    WHERE n.id = h.novel_id
)
WHERE h.novel_id IS NOT NULL 
  AND (h.novel_title IS NULL OR h.novel_title = '');

-- 2. 更新章节标题（通过 chapter_id 关联）
UPDATE ai_image_history h
SET chapter_title = (
    SELECT c.title 
    FROM chapters c 
    WHERE c.id = h.chapter_id
)
WHERE h.chapter_id IS NOT NULL 
  AND (h.chapter_title IS NULL OR h.chapter_title = '');

-- 3. 查看更新结果统计
SELECT 
    '更新后的记录统计' as description,
    COUNT(*) as total_records,
    SUM(CASE WHEN novel_title IS NOT NULL AND novel_title != '' THEN 1 ELSE 0 END) as with_novel_title,
    SUM(CASE WHEN novel_title IS NULL OR novel_title = '' THEN 1 ELSE 0 END) as without_novel_title,
    SUM(CASE WHEN chapter_title IS NOT NULL AND chapter_title != '' THEN 1 ELSE 0 END) as with_chapter_title,
    SUM(CASE WHEN chapter_title IS NULL OR chapter_title = '' THEN 1 ELSE 0 END) as without_chapter_title
FROM ai_image_history;

-- 4. 查看仍有问题的记录（可选）
SELECT 
    id,
    novel_id,
    novel_title,
    chapter_id,
    chapter_title,
    prompt,
    create_time
FROM ai_image_history
WHERE novel_title IS NULL 
   OR novel_title = ''
   OR chapter_title IS NULL 
   OR chapter_title = '';

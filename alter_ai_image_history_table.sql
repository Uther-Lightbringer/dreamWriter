-- 为 AI 图片生成历史记录表添加小说和章节信息字段
ALTER TABLE ai_image_history
ADD COLUMN novel_id BIGINT COMMENT '小说ID',
ADD COLUMN novel_title VARCHAR(500) COMMENT '小说标题',
ADD COLUMN chapter_id BIGINT COMMENT '章节ID',
ADD COLUMN chapter_title VARCHAR(500) COMMENT '章节标题';

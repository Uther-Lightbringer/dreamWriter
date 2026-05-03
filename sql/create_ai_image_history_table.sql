-- 创建 AI 图片生成历史记录表
CREATE TABLE IF NOT EXISTS ai_image_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prompt TEXT NOT NULL COMMENT 'AI 图片生成提示词',
    image_url VARCHAR(500) NOT NULL COMMENT '生成的图片 URL',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 图片生成历史记录表';

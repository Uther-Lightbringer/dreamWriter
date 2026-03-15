package org.zenithon.articlecollect.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.zenithon.articlecollect.dto.AiImageGenerationResponse;
import org.zenithon.articlecollect.util.ImageGeneratedUtils;

/**
 * 火山引擎文生图服务（基于 ImageGeneratedUtils）
 */
@Service
public class VolcEngineImageService {
    
    private static final Logger logger = LoggerFactory.getLogger(VolcEngineImageService.class);
    
    /**
     * 根据提示词生成图片
     * @param prompt AI 绘画提示词
     * @return 生成的图片 URL 或 null
     */
    public String generateImage(String prompt) {
        try {
            // 使用 ImageGeneratedUtils 生成图片
            AiImageGenerationResponse response = ImageGeneratedUtils.genearateAiImage(prompt, "uploads/images/generated");
            
            if (response.isSuccess()) {
                logger.info("火山引擎图片生成成功：{}", response.getImageUrl());
                return response.getImageUrl();
            } else {
                logger.error("火山引擎图片生成失败：{}", response.getMessage());
                return null;
            }
        } catch (Exception e) {
            logger.error("火山引擎图片生成失败：" + e.getMessage(), e);
            return null;
        }
    }
    

}

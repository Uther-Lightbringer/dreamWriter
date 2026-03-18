package org.zenithon.articlecollect.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.zenithon.articlecollect.entity.AiImageHistory;
import org.zenithon.articlecollect.repository.AiImageHistoryRepository;

import java.util.List;

/**
 * AI 图片历史记录服务类
 */
@Service
public class AiImageHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(AiImageHistoryService.class);

    private final AiImageHistoryRepository repository;

    public AiImageHistoryService(AiImageHistoryRepository repository) {
        this.repository = repository;
    }

    /**
     * 保存历史记录（防止重复）
     */
    public AiImageHistory saveHistory(String prompt, String imageUrl) {
        return saveHistory(prompt, imageUrl, null, null, null, null);
    }

    /**
     * 保存历史记录（带小说和章节信息）
     */
    public AiImageHistory saveHistory(String prompt, String imageUrl, Long novelId, String novelTitle, Long chapterId, String chapterTitle) {
        return saveHistory(prompt, imageUrl, novelId, novelTitle, chapterId, chapterTitle, null);
    }

    /**
     * 保存历史记录（带小说、章节和小说内容信息）
     */
    public AiImageHistory saveHistory(String prompt, String imageUrl, Long novelId, String novelTitle, Long chapterId, String chapterTitle, String novelContent) {
        try {
            // 先检查是否已存在相同的图片 URL
            List<AiImageHistory> allHistory = repository.findAllByOrderByCreateTimeDesc();
            for (AiImageHistory history : allHistory) {
                if (history.getImageUrl().equals(imageUrl)) {
                    logger.info("图片已存在，跳过保存：{}", imageUrl);
                    return history; // 返回已存在的记录
                }
            }

            // 不存在则创建新记录
            AiImageHistory history = new AiImageHistory(prompt, imageUrl, novelId, novelTitle, chapterId, chapterTitle, novelContent);
            AiImageHistory saved = repository.save(history);
            logger.info("历史记录保存成功：{}", imageUrl);
            return saved;
        } catch (Exception e) {
            logger.error("保存图片历史失败：" + e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取所有历史记录（按时间倒序）
     */
    public List<AiImageHistory> getAllHistory() {
        return repository.findAllByOrderByCreateTimeDesc();
    }

    /**
     * 删除历史记录
     */
    public void deleteHistory(Long id) {
        repository.deleteById(id);
    }
}

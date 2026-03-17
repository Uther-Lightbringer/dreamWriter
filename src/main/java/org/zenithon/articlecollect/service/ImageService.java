package org.zenithon.articlecollect.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.zenithon.articlecollect.entity.Novel;
import org.zenithon.articlecollect.entity.Chapter;
import org.zenithon.articlecollect.entity.CharacterCardEntity;
import org.zenithon.articlecollect.repository.NovelRepository;
import org.zenithon.articlecollect.repository.ChapterRepository;
import org.zenithon.articlecollect.repository.CharacterCardRepository;
import org.zenithon.articlecollect.util.FileUploadUtil;
import org.zenithon.articlecollect.dto.ImageUploadResponse;

import java.io.IOException;
import java.util.Optional;

/**
 * 图片服务类
 */
@Service
public class ImageService {
    
    @Autowired
    private NovelRepository novelRepository;
    
    @Autowired
    private ChapterRepository chapterRepository;
    
    @Autowired
    private CharacterCardRepository characterCardRepository;


    /**
     * 上传小说封面图片
     */
    @Transactional
    public ImageUploadResponse uploadNovelCoverImage(Long novelId, MultipartFile file) {
        try {
            // 验证小说是否存在
            Optional<Novel> novelOpt = novelRepository.findById(novelId);
            if (!novelOpt.isPresent()) {
                return new ImageUploadResponse(false, "小说不存在");
            }
            
            Novel novel = novelOpt.get();
            
            // 保存文件
            String imagePath = FileUploadUtil.saveImage(file, "novel", novelId);
            
            // 更新小说实体
            novel.setCoverImage(imagePath);
            novelRepository.save(novel);
            
            return new ImageUploadResponse(true, "封面图片上传成功", imagePath, novelId);
            
        } catch (IllegalArgumentException e) {
            return new ImageUploadResponse(false, e.getMessage());
        } catch (IOException e) {
            return new ImageUploadResponse(false, "文件保存失败: " + e.getMessage());
        } catch (Exception e) {
            return new ImageUploadResponse(false, "上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 上传章节插图
     */
    @Transactional
    public ImageUploadResponse uploadChapterImage(Long chapterId, MultipartFile file) {
        try {
            // 验证章节是否存在
            Optional<Chapter> chapterOpt = chapterRepository.findById(chapterId);
            if (!chapterOpt.isPresent()) {
                return new ImageUploadResponse(false, "章节不存在");
            }
                
            Chapter chapter = chapterOpt.get();
                
            // 获取小说信息用于生成文件名
            String novelName = "Unknown";
            String chapterName = "Unknown";
                
            if (chapter.getNovel() != null && chapter.getNovel().getTitle() != null) {
                novelName = sanitizeFileName(chapter.getNovel().getTitle());
            }
            if (chapter.getTitle() != null) {
                chapterName = sanitizeFileName(chapter.getTitle());
            }
                
            // 保存文件（使用自定义命名规则）
            String imagePath = FileUploadUtil.saveChapterImageWithCustomName(
                file, 
                novelName, 
                chapterName,
                chapterId
            );
                
            // 更新章节实体
            chapter.setChapterImage(imagePath);
            chapterRepository.save(chapter);
                
            return new ImageUploadResponse(true, "章节图片上传成功", imagePath, chapterId);
                
        } catch (IllegalArgumentException e) {
            return new ImageUploadResponse(false, e.getMessage());
        } catch (IOException e) {
            return new ImageUploadResponse(false, "文件保存失败：" + e.getMessage());
        } catch (Exception e) {
            return new ImageUploadResponse(false, "上传失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除小说封面图片
     */
    @Transactional
    public ImageUploadResponse deleteNovelCoverImage(Long novelId) {
        try {
            // 验证小说是否存在
            Optional<Novel> novelOpt = novelRepository.findById(novelId);
            if (!novelOpt.isPresent()) {
                return new ImageUploadResponse(false, "小说不存在");
            }
            
            Novel novel = novelOpt.get();
            
            // 删除文件
            boolean deleted = FileUploadUtil.deleteImage("novel", novelId);
            
            if (deleted) {
                // 清除数据库中的图片路径
                novel.setCoverImage(null);
                novelRepository.save(novel);
                return new ImageUploadResponse(true, "封面图片删除成功", null, novelId);
            } else {
                return new ImageUploadResponse(false, "图片文件不存在或删除失败");
            }
            
        } catch (Exception e) {
            return new ImageUploadResponse(false, "删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除章节插图
     */
    @Transactional
    public ImageUploadResponse deleteChapterImage(Long chapterId) {
        try {
            // 验证章节是否存在
            Optional<Chapter> chapterOpt = chapterRepository.findById(chapterId);
            if (!chapterOpt.isPresent()) {
                return new ImageUploadResponse(false, "章节不存在");
            }
            
            Chapter chapter = chapterOpt.get();
            
            // 删除文件
            boolean deleted = FileUploadUtil.deleteImage("chapter", chapterId);
            
            if (deleted) {
                // 清除数据库中的图片路径
                chapter.setChapterImage(null);
                chapterRepository.save(chapter);
                return new ImageUploadResponse(true, "章节图片删除成功", null, chapterId);
            } else {
                return new ImageUploadResponse(false, "图片文件不存在或删除失败");
            }
            
        } catch (Exception e) {
            return new ImageUploadResponse(false, "删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取小说封面图片路径
     */
    public String getNovelCoverImagePath(Long novelId) {
        Optional<Novel> novelOpt = novelRepository.findById(novelId);
        return novelOpt.map(Novel::getCoverImage).orElse(null);
    }
    
    /**
     * 获取章节插图路径
     */
    public String getChapterImagePath(Long chapterId) {
        Optional<Chapter> chapterOpt = chapterRepository.findById(chapterId);
        return chapterOpt.map(Chapter::getChapterImage).orElse(null);
    }
    
    /**
     * 上传角色卡图片
     */
    @Transactional
    public ImageUploadResponse uploadCharacterImage(Long characterId, MultipartFile file) {
        try {
            // 验证角色卡是否存在
            Optional<CharacterCardEntity> cardOpt = characterCardRepository.findById(characterId);
            if (!cardOpt.isPresent()) {
                return new ImageUploadResponse(false, "角色卡不存在");
            }
            
            CharacterCardEntity card = cardOpt.get();
            
            // 保存文件
            String imagePath = FileUploadUtil.saveImage(file, "character", characterId);
            
            // 更新角色卡实体
            card.setGeneratedImageUrl(imagePath);
            characterCardRepository.save(card);
            
            return new ImageUploadResponse(true, "角色卡图片上传成功", imagePath, characterId);
            
        } catch (IllegalArgumentException e) {
            return new ImageUploadResponse(false, e.getMessage());
        } catch (IOException e) {
            return new ImageUploadResponse(false, "文件保存失败：" + e.getMessage());
        } catch (Exception e) {
            return new ImageUploadResponse(false, "上传失败：" + e.getMessage());
        }
    }
    
    /**
     * 清理文件名，移除特殊字符和空格
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "Unknown";
        }
        // 替换特殊字符为下划线，保留中文、英文、数字
        return fileName.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "_");
    }
}

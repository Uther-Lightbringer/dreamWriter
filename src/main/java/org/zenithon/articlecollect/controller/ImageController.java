package org.zenithon.articlecollect.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.zenithon.articlecollect.dto.ImageUploadResponse;
import org.zenithon.articlecollect.service.ImageService;

/**
 * 图片管理控制器
 */
@RestController
@RequestMapping("/api/images")
public class ImageController {
    
    @Autowired
    private ImageService imageService;
    
    /**
     * 上传小说封面图片
     */
    @PostMapping("/novel/{novelId}/cover")
    public ResponseEntity<ImageUploadResponse> uploadNovelCoverImage(
            @PathVariable Long novelId,
            @RequestParam("file") MultipartFile file) {
        
        ImageUploadResponse response = imageService.uploadNovelCoverImage(novelId, file);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 上传章节插图
     */
    @PostMapping("/chapter/{chapterId}/image")
    public ResponseEntity<ImageUploadResponse> uploadChapterImage(
            @PathVariable Long chapterId,
            @RequestParam("file") MultipartFile file) {
        
        ImageUploadResponse response = imageService.uploadChapterImage(chapterId, file);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 删除小说封面图片
     */
    @DeleteMapping("/novel/{novelId}/cover")
    public ResponseEntity<ImageUploadResponse> deleteNovelCoverImage(@PathVariable Long novelId) {
        ImageUploadResponse response = imageService.deleteNovelCoverImage(novelId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 删除章节插图
     */
    @DeleteMapping("/chapter/{chapterId}/image")
    public ResponseEntity<ImageUploadResponse> deleteChapterImage(@PathVariable Long chapterId) {
        ImageUploadResponse response = imageService.deleteChapterImage(chapterId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取小说封面图片路径
     */
    @GetMapping("/novel/{novelId}/cover")
    public ResponseEntity<ImageUploadResponse> getNovelCoverImage(@PathVariable Long novelId) {
        String imagePath = imageService.getNovelCoverImagePath(novelId);
        ImageUploadResponse response = new ImageUploadResponse();
        response.setSuccess(true);
        response.setImageUrl(imagePath);
        response.setEntityId(novelId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取章节插图路径
     */
    @GetMapping("/chapter/{chapterId}/image")
    public ResponseEntity<ImageUploadResponse> getChapterImage(@PathVariable Long chapterId) {
        String imagePath = imageService.getChapterImagePath(chapterId);
        ImageUploadResponse response = new ImageUploadResponse();
        response.setSuccess(true);
        response.setImageUrl(imagePath);
        response.setEntityId(chapterId);
        return ResponseEntity.ok(response);
    }
}
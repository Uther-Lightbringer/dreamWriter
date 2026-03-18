package org.zenithon.articlecollect.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 文件上传工具类
 */
public class FileUploadUtil {
    
    // 图片存储基础路径
    private static final String UPLOAD_DIR = "uploads/images/";
    
    // 支持的图片格式
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"};
    
    // 最大文件大小 (10MB)
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    
    /**
     * 保存上传的图片文件
     * @param file 上传的文件
     * @param entityType 实体类型 ("novel" 或 "chapter")
     * @param entityId 实体ID
     * @return 保存的文件相对路径
     * @throws IOException 文件操作异常
     * @throws IllegalArgumentException 参数验证异常
     */
    public static String saveImage(MultipartFile file, String entityType, Long entityId) throws IOException {
        // 参数验证
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        
        if (entityType == null || entityId == null) {
            throw new IllegalArgumentException("实体类型和ID不能为空");
        }
        
        // 文件大小检查
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小不能超过10MB");
        }
        
        // 文件类型检查
        String fileName = file.getOriginalFilename();
        if (fileName == null || !isValidImageExtension(fileName)) {
            throw new IllegalArgumentException("只支持图片文件格式: jpg, jpeg, png, gif, bmp, webp");
        }
        
        // 创建目录结构: uploads/images/novel/1/ 或 uploads/images/chapter/1/
        String entityDir = entityType.toLowerCase();
        Path uploadPath = Paths.get(UPLOAD_DIR + entityDir + "/" + entityId);
        
        // 确保目录存在
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // 生成唯一文件名
        String extension = getFileExtension(fileName);
        String uniqueFileName = UUID.randomUUID().toString() + extension;
        Path filePath = uploadPath.resolve(uniqueFileName);
        
        // 保存文件
        Files.write(filePath, file.getBytes());
        
        // 返回相对路径用于Web访问
        return "/images/" + entityDir + "/" + entityId + "/" + uniqueFileName;
    }
    
    /**
     * 删除指定实体的图片文件
     * @param entityType 实体类型
     * @param entityId 实体ID
     * @return 是否删除成功
     */
    public static boolean deleteImage(String entityType, Long entityId) {
        try {
            String entityDir = entityType.toLowerCase();
            Path imagePath = Paths.get(UPLOAD_DIR + entityDir + "/" + entityId);
            
            if (Files.exists(imagePath)) {
                // 删除整个目录及其内容
                Files.walk(imagePath)
                    .sorted((path1, path2) -> path2.compareTo(path1)) // 逆序删除，先删文件再删目录
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // 记录日志但不中断删除过程
                            System.err.println("删除文件失败: " + path.toString());
                        }
                    });
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("删除图片目录失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查文件扩展名是否为支持的图片格式
     */
    private static boolean isValidImageExtension(String fileName) {
        String lowerFileName = fileName.toLowerCase();
        for (String ext : ALLOWED_EXTENSIONS) {
            if (lowerFileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取文件扩展名
     */
    private static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex).toLowerCase() : "";
    }
    
    /**
     * 获取上传目录的绝对路径
     */
    public static String getUploadDirectory() {
        return UPLOAD_DIR;
    }
    
    /**
     * 保存章节图片（使用自定义命名规则）
     * @param file 上传的文件
     * @param novelName 小说名称
     * @param chapterName 章节名称
     * @param chapterId 章节 ID
     * @return 保存的文件相对路径
     * @throws IOException 文件操作异常
     * @throws IllegalArgumentException 参数验证异常
     */
    public static String saveChapterImageWithCustomName(MultipartFile file, String novelName, String chapterName, Long chapterId) throws IOException {
        // 参数验证
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        
        if (novelName == null || chapterName == null || chapterId == null) {
            throw new IllegalArgumentException("小说名、章节名和 ID 不能为空");
        }
        
        // 文件大小检查
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小不能超过 10MB");
        }
        
        // 文件类型检查
        String fileName = file.getOriginalFilename();
        if (fileName == null || !isValidImageExtension(fileName)) {
            throw new IllegalArgumentException("只支持图片文件格式：jpg, jpeg, png, gif, bmp, webp");
        }
        
        // 创建目录结构：uploads/images/chapter/{chapterId}/
        Path uploadPath = Paths.get(UPLOAD_DIR + "chapter/" + chapterId);
        
        // 确保目录存在
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // 生成文件名：{novelName}-{chapterName}-{4 位数字 id}.{extension}
        String extension = getFileExtension(fileName);
        String fourDigitId = String.valueOf(Math.random() * 10000);
        String customFileName = novelName + "-" + chapterName + "-" + fourDigitId + extension;
        
        // 限制文件名长度（可选）
        if (customFileName.length() > 200) {
            customFileName = customFileName.substring(0, 200) + extension;
        }
        
        Path filePath = uploadPath.resolve(customFileName);
        
        // 保存文件
        Files.write(filePath, file.getBytes());
        
        // 返回相对路径用于 Web 访问
        return "/images/chapter/" + chapterId + "/" + customFileName;
    }

    /**
     * 保存章节图片（使用自定义命名规则）
     * @param file 上传的文件
     * @param novelName 小说名称
     * @param chapterName 章节名称
     * @param chapterId 章节 ID
     * @return 保存的文件相对路径
     * @throws IOException 文件操作异常
     * @throws IllegalArgumentException 参数验证异常
     */
    public static String saveImageWithCustomName(MultipartFile file, String novelName, String chapterName, Long chapterId) throws IOException {
        // 参数验证
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        if (novelName == null || chapterName == null || chapterId == null) {
            throw new IllegalArgumentException("小说名、章节名和 ID 不能为空");
        }

        // 文件大小检查
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小不能超过 10MB");
        }

        // 文件类型检查
        String fileName = file.getOriginalFilename();
        if (fileName == null || !isValidImageExtension(fileName)) {
            throw new IllegalArgumentException("只支持图片文件格式：jpg, jpeg, png, gif, bmp, webp");
        }

        // 创建目录结构：uploads/images/chapterExt/{chapterId}/
        Path uploadPath = Paths.get(UPLOAD_DIR + "chapterExt/" + chapterId);

        // 确保目录存在
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 生成文件名：{novelName}-{chapterName}-{4 位数字 id}.{extension}
        String extension = getFileExtension(fileName);
        String fourDigitId = String.valueOf(Math.random() * 10000);
        String customFileName = novelName + "-" + chapterName + "-" + fourDigitId + extension;

        // 限制文件名长度（可选）
        if (customFileName.length() > 200) {
            customFileName = customFileName.substring(0, 200) + extension;
        }

        Path filePath = uploadPath.resolve(customFileName);

        // 保存文件
        Files.write(filePath, file.getBytes());

        // 返回相对路径用于 Web 访问
        return "/images/chapterExt/" + chapterId + "/" + customFileName;
    }
    
    /**
     * 保存 AI 自动生成的图片到章节目录
     * @param imageData 图片二进制数据
     * @param chapterId 章节 ID
     * @param chapterTitle 章节标题
     * @return 保存的文件相对路径
     * @throws IOException 文件操作异常
     */
    public static String saveAutoGeneratedImage(byte[] imageData, Long chapterId, String chapterTitle) throws IOException {
        // 参数验证
        if (imageData == null || imageData.length == 0) {
            throw new IllegalArgumentException("图片数据不能为空");
        }
        
        if (chapterId == null || chapterTitle == null) {
            throw new IllegalArgumentException("章节 ID 和标题不能为空");
        }
        
        // 创建目录结构：uploads/images/auto-generated/{chapterId}/
        Path uploadPath = Paths.get(UPLOAD_DIR + "auto-generated/" + chapterId);
        
        // 确保目录存在
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // 生成文件名：auto-{时间戳 -4 位随机数}.jpg
        long timestamp = System.currentTimeMillis();
        String randomId = String.format("%04d", (int)(Math.random() * 10000));
        String fileName = "auto-" + timestamp + "-" + randomId + ".jpg";
        
        Path filePath = uploadPath.resolve(fileName);
        
        // 保存图片
        Files.write(filePath, imageData);
        
        System.out.println("AI 自动生成图片已保存：" + filePath.toString());
        
        // 返回相对路径用于 Web 访问
        return "/images/auto-generated/" + chapterId + "/" + fileName;
    }

    /**
     * 保存角色卡图片
     * @param imageData 图片二进制数据
     * @param characterName 角色名称
     * @return 保存的文件相对路径
     * @throws IOException 文件操作异常
     */
    public static String saveCharacterCardImage(byte[] imageData, String characterName) throws IOException {
        // 参数验证
        if (imageData == null || imageData.length == 0) {
            throw new IllegalArgumentException("图片数据不能为空");
        }

        if (characterName == null || characterName.trim().isEmpty()) {
            throw new IllegalArgumentException("角色名称不能为空");
        }

        // 创建目录结构：uploads/images/character-card/
        Path uploadPath = Paths.get(UPLOAD_DIR + "character-card");

        // 确保目录存在
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 生成文件名：{角色名}-{时间戳}-{4 位随机数}.jpg
        long timestamp = System.currentTimeMillis();
        String randomId = String.format("%04d", (int)(Math.random() * 10000));
        String safeCharacterName = characterName.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "_");
        String fileName = safeCharacterName + "-" + timestamp + "-" + randomId + ".jpg";

        Path filePath = uploadPath.resolve(fileName);

        // 保存图片
        Files.write(filePath, imageData);

        System.out.println("角色卡图片已保存：" + filePath.toString());

        // 返回相对路径用于 Web 访问
        return "/images/character-card/" + fileName;
    }
}
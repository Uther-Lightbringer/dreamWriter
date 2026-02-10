package org.zenithon.articlecollect.dto;

/**
 * 图片上传响应DTO
 */
public class ImageUploadResponse {
    private boolean success;
    private String message;
    private String imageUrl;
    private Long entityId;

    public ImageUploadResponse() {}

    public ImageUploadResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public ImageUploadResponse(boolean success, String message, String imageUrl, Long entityId) {
        this.success = success;
        this.message = message;
        this.imageUrl = imageUrl;
        this.entityId = entityId;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }
}
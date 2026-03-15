package org.zenithon.articlecollect.dto;

/**
 * AI 图片生成响应 DTO
 */
public class AiImageGenerationResponse {
    private boolean success;
    private String message;
    private String imageUrl;

    public AiImageGenerationResponse() {}

    public AiImageGenerationResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public AiImageGenerationResponse(boolean success, String message, String imageUrl) {
        this.success = success;
        this.message = message;
        this.imageUrl = imageUrl;
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
}

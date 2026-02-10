package org.zenithon.articlecollect.dto;

/**
 * 图片删除请求DTO
 */
public class ImageDeleteRequest {
    private Long entityId;
    private String entityType; // "novel" 或 "chapter"

    public ImageDeleteRequest() {}

    public ImageDeleteRequest(Long entityId, String entityType) {
        this.entityId = entityId;
        this.entityType = entityType;
    }

    // Getters and Setters
    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
}
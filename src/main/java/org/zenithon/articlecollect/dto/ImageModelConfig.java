package org.zenithon.articlecollect.dto;

/**
 * 图片模型配置 DTO
 */
public class ImageModelConfig {

    private String id;
    private String name;
    private String description;

    public ImageModelConfig() {
    }

    public ImageModelConfig(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

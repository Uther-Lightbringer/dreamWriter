package org.zenithon.articlecollect.dto;

/**
 * 世界观更新请求 DTO
 */
public class WorldViewRequest {
    
    private String worldView;
    
    public WorldViewRequest() {
    }
    
    public WorldViewRequest(String worldView) {
        this.worldView = worldView;
    }
    
    public String getWorldView() {
        return worldView;
    }
    
    public void setWorldView(String worldView) {
        this.worldView = worldView;
    }
}

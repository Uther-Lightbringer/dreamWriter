package org.zenithon.articlecollect.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 角色卡关系信息 DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CharacterCardRelationship {
    
    private String targetId;
    private String targetName;
    private String relationship;
    
    public CharacterCardRelationship() {
    }
    
    // Getters and Setters
    public String getTargetId() {
        return targetId;
    }
    
    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }
    
    public String getTargetName() {
        return targetName;
    }
    
    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }
    
    public String getRelationship() {
        return relationship;
    }
    
    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }
}

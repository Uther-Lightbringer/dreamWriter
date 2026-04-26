package org.zenithon.articlecollect.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.ToString;

/**
 * 角色卡外观信息 DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CharacterCardAppearance {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private String height;
    private String hair;
    private String eyes;
    private String face;  // 脸型描述
    private String build;
    private String distinguishingFeatures;
    private String clothing;  // 穿着风格
    
    public CharacterCardAppearance() {
    }
    
    // Getters and Setters
    public String getHeight() {
        return height;
    }
    
    public void setHeight(String height) {
        this.height = height;
    }
    
    public String getHair() {
        return hair;
    }
    
    public void setHair(String hair) {
        this.hair = hair;
    }
    
    public String getEyes() {
        return eyes;
    }
    
    public void setEyes(String eyes) {
        this.eyes = eyes;
    }

    public String getFace() {
        return face;
    }

    public void setFace(String face) {
        this.face = face;
    }

    public String getBuild() {
        return build;
    }
    
    public void setBuild(String build) {
        this.build = build;
    }
    
    public String getDistinguishingFeatures() {
        return distinguishingFeatures;
    }
    
    public void setDistinguishingFeatures(String distinguishingFeatures) {
        this.distinguishingFeatures = distinguishingFeatures;
    }

    public String getClothing() {
        return clothing;
    }

    public void setClothing(String clothing) {
        this.clothing = clothing;
    }


    @Override
    public String toString() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (Exception e) {
            return "CharacterCardAppearance{}";
        }
    }
}

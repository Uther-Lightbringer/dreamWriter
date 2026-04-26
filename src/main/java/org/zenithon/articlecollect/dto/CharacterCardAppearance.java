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
    private String clothing;  // 服装
    private String legwear;   // 腿部穿着（丝袜、袜子等）
    private String shoes;     // 鞋子
    private String accessories; // 配饰

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

    public String getLegwear() {
        return legwear;
    }

    public void setLegwear(String legwear) {
        this.legwear = legwear;
    }

    public String getShoes() {
        return shoes;
    }

    public void setShoes(String shoes) {
        this.shoes = shoes;
    }

    public String getAccessories() {
        return accessories;
    }

    public void setAccessories(String accessories) {
        this.accessories = accessories;
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

package org.zenithon.articlecollect.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 结构化角色卡 DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CharacterCard {
    
    private Long id;
    private String name;
    private List<String> alternativeNames;
    private Integer age;
    private String gender;
    private String occupation;
    private CharacterCardAppearance appearance;
    private String appearanceDescription; // 外貌描述文本（用于 AI 文生图）
    private String generatedImageUrl; // 生成的角色图片 URL
    private Integer promptVersion; // AI 绘画提示词版本号
    private Integer imageVersion; // 生成的图片版本号
    private String personality;
    private String background;
    private List<CharacterCardRelationship> relationships;
    private String notes;
    private String role; // 角色类型：protagonist(主角), supporting(配角), etc.

    public CharacterCard() {
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public List<String> getAlternativeNames() {
        return alternativeNames;
    }
    
    public void setAlternativeNames(List<String> alternativeNames) {
        this.alternativeNames = alternativeNames;
    }
    
    public Integer getAge() {
        return age;
    }
    
    public void setAge(Integer age) {
        this.age = age;
    }
    
    public String getGender() {
        return gender;
    }
    
    public void setGender(String gender) {
        this.gender = gender;
    }
    
    public String getOccupation() {
        return occupation;
    }
    
    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public CharacterCardAppearance getAppearance() {
        return appearance;
    }

    public void setAppearance(CharacterCardAppearance appearance) {
        this.appearance = appearance;
    }

    public String getAppearanceDescription() {
        return appearanceDescription;
    }
    
    public void setAppearanceDescription(String appearanceDescription) {
        this.appearanceDescription = appearanceDescription;
    }
    
    public String getGeneratedImageUrl() {
        return generatedImageUrl;
    }
    
    public void setGeneratedImageUrl(String generatedImageUrl) {
        this.generatedImageUrl = generatedImageUrl;
    }
    
    public Integer getPromptVersion() {
        return promptVersion;
    }
    
    public void setPromptVersion(Integer promptVersion) {
        this.promptVersion = promptVersion;
    }
    
    public Integer getImageVersion() {
        return imageVersion;
    }
    
    public void setImageVersion(Integer imageVersion) {
        this.imageVersion = imageVersion;
    }
    
    public String getPersonality() {
        return personality;
    }
    
    public void setPersonality(String personality) {
        this.personality = personality;
    }
    
    public String getBackground() {
        return background;
    }
    
    public void setBackground(String background) {
        this.background = background;
    }
    
    public List<CharacterCardRelationship> getRelationships() {
        return relationships;
    }
    
    public void setRelationships(List<CharacterCardRelationship> relationships) {
        this.relationships = relationships;
    }
    
    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}

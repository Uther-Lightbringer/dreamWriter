package org.zenithon.articlecollect.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色卡实体类（数据库表）
 */
@Entity
@Table(name = "character_cards")
public class CharacterCardEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 255)
    private String name; // 角色姓名
    
    @Column(length = 1000)
    private String alternativeNames; // 别名，JSON 数组格式存储
    
    @Column
    private Integer age; // 年龄
    
    @Column(length = 50)
    private String gender; // 性别
    
    @Column(length = 255)
    private String occupation; // 职业
    
    // 外貌特征 - 使用 JSON 格式存储
    @Column(name = "appearance_json", columnDefinition = "TEXT")
    private String appearanceJson;
    
    @Column(name = "appearance_description", columnDefinition = "TEXT")
    private String appearanceDescription; // 外貌描述文本（用于 AI 文生图）
    
    @Column(name = "generated_image_url", length = 500)
    private String generatedImageUrl; // 生成的角色图片 URL
    
    @Column(name = "prompt_version")
    private Integer promptVersion; // AI 绘画提示词版本号
    
    @Column(name = "image_version")
    private Integer imageVersion; // 生成的图片版本号
    
    @Column(name = "personality", columnDefinition = "TEXT")
    private String personality; // 性格描述
    
    @Column(name = "background", columnDefinition = "TEXT")
    private String background; // 背景故事
    
    // 人际关系 - 使用 JSON 格式存储
    @Column(name = "relationships", columnDefinition = "TEXT")
    private String relationshipsJson;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes; // 备注
    
    @Column(name = "novel_id", nullable = false)
    private Long novelId; // 所属小说 ID
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "novel_id", insertable = false, updatable = false)
    @JsonIgnore
    private Novel novel; // 关联的小说
    
    @Column(name = "create_time")
    private LocalDateTime createTime;
    
    @Column(name = "update_time")
    private LocalDateTime updateTime;
    
    @Column(name = "sort_order")
    private Integer sortOrder; // 排序顺序
    
    public CharacterCardEntity() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
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
    
    public String getAlternativeNames() {
        return alternativeNames;
    }
    
    public void setAlternativeNames(String alternativeNames) {
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
    
    public String getAppearanceJson() {
        return appearanceJson;
    }
    
    public void setAppearanceJson(String appearanceJson) {
        this.appearanceJson = appearanceJson;
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
    
    public String getRelationshipsJson() {
        return relationshipsJson;
    }
    
    public void setRelationshipsJson(String relationshipsJson) {
        this.relationshipsJson = relationshipsJson;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public Long getNovelId() {
        return novelId;
    }
    
    public void setNovelId(Long novelId) {
        this.novelId = novelId;
    }
    
    public Novel getNovel() {
        return novel;
    }
    
    public void setNovel(Novel novel) {
        this.novel = novel;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
    
    public Integer getSortOrder() {
        return sortOrder;
    }
    
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
    
    @Override
    public String toString() {
        return "CharacterCardEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", novelId=" + novelId +
                '}';
    }
}

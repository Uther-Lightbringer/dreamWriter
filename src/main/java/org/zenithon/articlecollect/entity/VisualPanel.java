package org.zenithon.articlecollect.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "visual_panels")
public class VisualPanel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_id", nullable = false)
    private Long workId;

    @Column(name = "panel_number", nullable = false)
    private Integer panelNumber;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String scene;

    @Column(name = "camera_angle")
    private String cameraAngle;

    @Column(columnDefinition = "TEXT")
    private String dialogue;

    @Column(name = "sound_effect")
    private String soundEffect;

    @Column(columnDefinition = "TEXT")
    private String action;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getWorkId() { return workId; }
    public void setWorkId(Long workId) { this.workId = workId; }
    public Integer getPanelNumber() { return panelNumber; }
    public void setPanelNumber(Integer panelNumber) { this.panelNumber = panelNumber; }
    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
    public String getCameraAngle() { return cameraAngle; }
    public void setCameraAngle(String cameraAngle) { this.cameraAngle = cameraAngle; }
    public String getDialogue() { return dialogue; }
    public void setDialogue(String dialogue) { this.dialogue = dialogue; }
    public String getSoundEffect() { return soundEffect; }
    public void setSoundEffect(String soundEffect) { this.soundEffect = soundEffect; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
}

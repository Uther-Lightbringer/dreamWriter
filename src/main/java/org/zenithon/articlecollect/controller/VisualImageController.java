package org.zenithon.articlecollect.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zenithon.articlecollect.entity.VisualImage;
import org.zenithon.articlecollect.entity.VisualImageGroup;
import org.zenithon.articlecollect.entity.VisualPanel;
import org.zenithon.articlecollect.entity.VisualWork;
import org.zenithon.articlecollect.repository.VisualImageGroupRepository;
import org.zenithon.articlecollect.repository.VisualImageRepository;
import org.zenithon.articlecollect.repository.VisualPanelRepository;
import org.zenithon.articlecollect.repository.VisualWorkRepository;
import org.zenithon.articlecollect.service.EvoLinkImageService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/visual")
public class VisualImageController {

    private static final Logger logger = LoggerFactory.getLogger(VisualImageController.class);

    @Autowired
    private VisualWorkRepository visualWorkRepository;

    @Autowired
    private VisualPanelRepository visualPanelRepository;

    @Autowired
    private VisualImageGroupRepository imageGroupRepository;

    @Autowired
    private VisualImageRepository imageRepository;

    @Autowired
    private EvoLinkImageService evoLinkImageService;

    /**
     * 为作品的所有分镜生成图片（使用 EvoLink）
     */
    @PostMapping("/{workId}/generate-images")
    public ResponseEntity<Map<String, Object>> generateImages(@PathVariable Long workId) {
        VisualWork work = visualWorkRepository.findById(workId).orElse(null);
        if (work == null) {
            return ResponseEntity.notFound().build();
        }

        List<VisualPanel> panels = visualPanelRepository.findByWorkIdOrderByPanelNumberAsc(workId);
        if (panels.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", "该作品还没有分镜，请先添加分镜"
            ));
        }

        // 创建图片分组
        VisualImageGroup group = new VisualImageGroup();
        group.setWorkId(workId);
        group.setGroupName("生成于 " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")));
        group = imageGroupRepository.save(group);

        // 为每个分镜创建生成任务
        List<Map<String, Object>> tasks = new ArrayList<>();

        for (VisualPanel panel : panels) {
            try {
                // 构建提示词
                StringBuilder prompt = new StringBuilder();
                if (panel.getScene() != null && !panel.getScene().isEmpty()) {
                    prompt.append(panel.getScene());
                }
                if (panel.getCameraAngle() != null && !panel.getCameraAngle().isEmpty()) {
                    prompt.append(", ").append(panel.getCameraAngle()).append(" shot");
                }
                if (panel.getAction() != null && !panel.getAction().isEmpty()) {
                    prompt.append(", ").append(panel.getAction());
                }
                prompt.append(", manga panel, comic art, high quality");

                String promptStr = prompt.toString();

                // 调用 EvoLink 创建任务
                String taskId = evoLinkImageService.generateImage(promptStr, "16:9", null);

                tasks.add(Map.of(
                    "panelId", panel.getId(),
                    "panelNumber", panel.getPanelNumber(),
                    "taskId", taskId,
                    "prompt", promptStr
                ));

                logger.info("创建分镜图片任务: panelId={}, taskId={}", panel.getId(), taskId);
            } catch (Exception e) {
                logger.error("创建分镜图片任务失败: panelId={}", panel.getId(), e);
                tasks.add(Map.of(
                    "panelId", panel.getId(),
                    "panelNumber", panel.getPanelNumber(),
                    "error", e.getMessage()
                ));
            }
        }

        return ResponseEntity.ok(Map.of(
            "success", true,
            "groupId", group.getId(),
            "groupName", group.getGroupName(),
            "tasks", tasks
        ));
    }

    /**
     * 查询单个任务状态
     */
    @GetMapping("/task-status/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskStatus(@PathVariable String taskId) {
        try {
            EvoLinkImageService.TaskStatus status = evoLinkImageService.getTaskStatus(taskId);
            return ResponseEntity.ok(Map.of(
                "status", status.getStatus(),
                "progress", status.getProgress(),
                "imageUrl", status.getImageUrl() != null ? status.getImageUrl() : "",
                "error", status.getError() != null ? status.getError() : ""
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "status", "failed",
                "error", e.getMessage()
            ));
        }
    }

    /**
     * 轮询图片生成状态并保存结果
     */
    @PostMapping("/{workId}/poll-group/{groupId}")
    public ResponseEntity<Map<String, Object>> pollGroupStatus(
            @PathVariable Long workId,
            @PathVariable Long groupId) {

        VisualImageGroup group = imageGroupRepository.findById(groupId).orElse(null);
        if (group == null) {
            return ResponseEntity.notFound().build();
        }

        List<VisualImage> images = imageRepository.findByGroupIdOrderByPanelNumberAsc(groupId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groupId", groupId);
        result.put("total", images.size());
        result.put("completed", images.stream().filter(i -> i.getImageUrl() != null).count());
        result.put("images", images.stream().map(i -> Map.of(
            "id", i.getId(),
            "panelNumber", i.getPanelNumber() != null ? i.getPanelNumber() : 0,
            "imageUrl", i.getImageUrl() != null ? i.getImageUrl() : "",
            "status", i.getImageUrl() != null ? "completed" : "pending"
        )).toList());

        return ResponseEntity.ok(result);
    }

    /**
     * 保存生成的图片到分组
     */
    @PostMapping("/image-groups/{groupId}/save-image")
    public ResponseEntity<Map<String, Object>> saveImage(
            @PathVariable Long groupId,
            @RequestBody Map<String, Object> request) {

        VisualImageGroup group = imageGroupRepository.findById(groupId).orElse(null);
        if (group == null) {
            return ResponseEntity.notFound().build();
        }

        VisualImage image = new VisualImage();
        image.setGroupId(groupId);
        if (request.get("panelId") != null) {
            image.setPanelId(((Number) request.get("panelId")).longValue());
        }
        if (request.get("panelNumber") != null) {
            image.setPanelNumber(((Number) request.get("panelNumber")).intValue());
        }
        image.setImageUrl((String) request.get("imageUrl"));
        image.setPrompt((String) request.get("prompt"));
        image = imageRepository.save(image);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "imageId", image.getId()
        ));
    }

    /**
     * 获取作品的所有图片分组
     */
    @GetMapping("/{workId}/image-groups")
    public ResponseEntity<List<Map<String, Object>>> getImageGroups(@PathVariable Long workId) {
        List<VisualImageGroup> groups = imageGroupRepository.findByWorkIdOrderByCreateTimeDesc(workId);

        List<Map<String, Object>> result = groups.stream().map(g -> {
            List<VisualImage> images = imageRepository.findByGroupIdOrderByPanelNumberAsc(g.getId());
            Map<String, Object> groupMap = new LinkedHashMap<>();
            groupMap.put("id", g.getId());
            groupMap.put("groupName", g.getGroupName());
            groupMap.put("createTime", g.getCreateTime() != null ? g.getCreateTime().toString() : "");
            groupMap.put("imageCount", images.size());
            groupMap.put("completedCount", images.stream().filter(i -> i.getImageUrl() != null).count());
            // 返回第一张图片作为封面
            if (!images.isEmpty() && images.get(0).getImageUrl() != null) {
                groupMap.put("coverImage", images.get(0).getImageUrl());
            }
            return groupMap;
        }).toList();

        return ResponseEntity.ok(result);
    }

    /**
     * 获取分组内的图片列表
     */
    @GetMapping("/image-groups/{groupId}")
    public ResponseEntity<Map<String, Object>> getGroupImages(@PathVariable Long groupId) {
        VisualImageGroup group = imageGroupRepository.findById(groupId).orElse(null);
        if (group == null) {
            return ResponseEntity.notFound().build();
        }

        List<VisualImage> images = imageRepository.findByGroupIdOrderByPanelNumberAsc(groupId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groupId", group.getId());
        result.put("groupName", group.getGroupName());
        result.put("createTime", group.getCreateTime() != null ? group.getCreateTime().toString() : "");
        result.put("images", images.stream().map(i -> {
            Map<String, Object> img = new LinkedHashMap<>();
            img.put("id", i.getId());
            img.put("panelId", i.getPanelId());
            img.put("panelNumber", i.getPanelNumber());
            img.put("imageUrl", i.getImageUrl());
            img.put("prompt", i.getPrompt());
            return img;
        }).toList());

        return ResponseEntity.ok(result);
    }

    /**
     * 删除图片分组
     */
    @DeleteMapping("/image-groups/{groupId}")
    public ResponseEntity<Map<String, Object>> deleteGroup(@PathVariable Long groupId) {
        VisualImageGroup group = imageGroupRepository.findById(groupId).orElse(null);
        if (group == null) {
            return ResponseEntity.notFound().build();
        }

        imageRepository.deleteByGroupId(groupId);
        imageGroupRepository.delete(group);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "分组已删除"
        ));
    }
}

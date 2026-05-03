package org.zenithon.articlecollect.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.zenithon.articlecollect.entity.VisualImage;
import org.zenithon.articlecollect.entity.VisualImageGroup;
import org.zenithon.articlecollect.entity.VisualPanel;
import org.zenithon.articlecollect.entity.VisualWork;
import org.zenithon.articlecollect.repository.VisualImageGroupRepository;
import org.zenithon.articlecollect.repository.VisualImageRepository;
import org.zenithon.articlecollect.repository.VisualPanelRepository;
import org.zenithon.articlecollect.repository.VisualWorkRepository;
import org.zenithon.articlecollect.util.FileUploadUtil;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 视觉叙事图片异步生成服务
 */
@Service
public class VisualImageAsyncService {

    private static final Logger logger = LoggerFactory.getLogger(VisualImageAsyncService.class);

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

    @Autowired
    private ObjectMapper objectMapper;

    // 任务进度存储：groupId -> TaskProgress
    private final ConcurrentHashMap<Long, TaskProgress> taskProgressMap = new ConcurrentHashMap<>();

    /**
     * 任务进度
     */
    public static class TaskProgress {
        private String status = "pending"; // pending, generating_outline, generating_images, completed, failed
        private int totalPanels = 0;
        private int completedPanels = 0;
        private String outline; // 视觉大纲
        private String errorMessage;
        private Long groupId;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getTotalPanels() { return totalPanels; }
        public void setTotalPanels(int totalPanels) { this.totalPanels = totalPanels; }
        public int getCompletedPanels() { return completedPanels; }
        public void setCompletedPanels(int completedPanels) { this.completedPanels = completedPanels; }
        public String getOutline() { return outline; }
        public void setOutline(String outline) { this.outline = outline; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public Long getGroupId() { return groupId; }
        public void setGroupId(Long groupId) { this.groupId = groupId; }

        public int getProgress() {
            if (totalPanels == 0) return 0;
            return (int) ((completedPanels * 100.0) / totalPanels);
        }
    }

    /**
     * 获取任务进度
     */
    public TaskProgress getTaskProgress(Long groupId) {
        return taskProgressMap.get(groupId);
    }

    /**
     * 异步生成视觉大纲和图片
     */
    @Async("characterCardTaskExecutor")
    public void generateOutlineAndImagesAsync(Long workId, Long groupId) {
        TaskProgress progress = new TaskProgress();
        progress.setGroupId(groupId);
        progress.setStatus("generating_outline");
        taskProgressMap.put(groupId, progress);

        try {
            // 1. 获取作品和分镜
            VisualWork work = visualWorkRepository.findById(workId)
                .orElseThrow(() -> new RuntimeException("作品不存在"));

            List<VisualPanel> panels = visualPanelRepository.findByWorkIdOrderByPanelNumberAsc(workId);
            progress.setTotalPanels(panels.size());

            if (panels.isEmpty()) {
                progress.setStatus("completed");
                return;
            }

            // 2. 生成视觉大纲
            logger.info("开始生成视觉大纲: workId={}", workId);
            String outline = generateVisualOutline(work, panels);
            progress.setOutline(outline);

            // 保存大纲到作品
            work.setVisualOutline(outline);
            visualWorkRepository.save(work);

            logger.info("视觉大纲生成完成: workId={}", workId);

            // 3. 解析大纲获取每个分镜的风格定义
            Map<String, Object> outlineData = objectMapper.readValue(outline, new TypeReference<>() {});
            String globalStyle = objectMapper.writeValueAsString(outlineData.get("globalStyle"));
            List<Map<String, Object>> panelStyles = (List<Map<String, Object>>) outlineData.get("panels");

            // 4. 异步并行生成图片
            progress.setStatus("generating_images");

            for (int i = 0; i < panels.size(); i++) {
                VisualPanel panel = panels.get(i);
                Map<String, Object> panelStyle = i < panelStyles.size() ? panelStyles.get(i) : null;

                try {
                    // 构建提示词：全局风格 + 分镜风格 + 分镜内容
                    String prompt = buildImagePrompt(globalStyle, panelStyle, panel);

                    // 调用 EvoLink 生成图片
                    String taskId = evoLinkImageService.generateImage(prompt, "16:9", null);

                    // 轮询等待完成
                    String imageUrl = pollForImage(taskId);

                    if (imageUrl != null) {
                        // 下载并保存到本地
                        String localPath = FileUploadUtil.downloadAndSaveImage(imageUrl, "visual", workId);

                        // 保存到数据库
                        VisualImage image = new VisualImage();
                        image.setGroupId(groupId);
                        image.setPanelId(panel.getId());
                        image.setPanelNumber(panel.getPanelNumber());
                        image.setImageUrl(localPath);
                        image.setPrompt(prompt);
                        imageRepository.save(image);

                        // 更新分镜图片
                        panel.setImageUrl(localPath);
                        visualPanelRepository.save(panel);

                        progress.setCompletedPanels(progress.getCompletedPanels() + 1);
                        logger.info("分镜{}图片生成完成: {}", panel.getPanelNumber(), localPath);
                    }
                } catch (Exception e) {
                    logger.error("分镜{}图片生成失败: {}", panel.getPanelNumber(), e.getMessage());
                    // 继续处理下一个分镜
                }
            }

            progress.setStatus("completed");
            logger.info("所有分镜图片生成完成: workId={}, completed={}/{}",
                workId, progress.getCompletedPanels(), progress.getTotalPanels());

        } catch (Exception e) {
            logger.error("异步生成失败: {}", e.getMessage(), e);
            progress.setStatus("failed");
            progress.setErrorMessage(e.getMessage());
        }
    }

    /**
     * 生成视觉大纲（调用 AI）
     */
    private String generateVisualOutline(VisualWork work, List<VisualPanel> panels) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一位专业的视觉叙事导演。请为以下视觉叙事作品生成一个统一的视觉大纲。\n\n");
        prompt.append("作品类型：").append(work.getVisualType()).append("\n");
        prompt.append("作品标题：").append(work.getTitle()).append("\n");
        if (work.getDescription() != null) {
            prompt.append("作品简介：").append(work.getDescription()).append("\n");
        }
        prompt.append("\n分镜列表：\n");

        for (VisualPanel panel : panels) {
            prompt.append("\n分镜").append(panel.getPanelNumber()).append("：\n");
            prompt.append("- 画面：").append(panel.getScene()).append("\n");
            if (panel.getCameraAngle() != null) {
                prompt.append("- 镜头：").append(panel.getCameraAngle()).append("\n");
            }
            if (panel.getAction() != null) {
                prompt.append("- 动作：").append(panel.getAction()).append("\n");
            }
            if (panel.getDialogue() != null) {
                prompt.append("- 对话：").append(panel.getDialogue()).append("\n");
            }
        }

        prompt.append("\n请生成 JSON 格式的视觉大纲，包含：\n");
        prompt.append("1. globalStyle: 全局风格定义（artStyle 画风、colorPalette 色调、lighting 整体光影）\n");
        prompt.append("2. panels: 每个分镜的视觉要点（visualFocus 视觉焦点、composition 构图、lighting 光影、mood 情绪氛围）\n\n");
        prompt.append("要求：\n");
        prompt.append("- 风格要统一，适合").append(work.getVisualType()).append("\n");
        prompt.append("- 每个分镜的视觉要点要具体，便于 AI 绘图\n");
        prompt.append("- 只返回 JSON，不要其他内容");

        String systemMessage = "你是专业的视觉叙事导演，擅长为漫画、分镜、绘本等作品设计统一的视觉风格。请只返回 JSON 格式的内容。";

        try {
            String response = callDeepSeekChat(prompt.toString(), systemMessage);
            // 提取 JSON 部分
            int jsonStart = response.indexOf('{');
            int jsonEnd = response.lastIndexOf('}');
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                return response.substring(jsonStart, jsonEnd + 1);
            }
            return response;
        } catch (Exception e) {
            logger.error("生成视觉大纲失败: {}", e.getMessage());
            // 返回默认大纲
            return getDefaultOutline(panels);
        }
    }

    /**
     * 获取默认大纲（AI 生成失败时使用）
     */
    private String getDefaultOutline(List<VisualPanel> panels) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"globalStyle\":{\"artStyle\":\"日系漫画风格\",\"colorPalette\":\"柔和暖色调\",\"lighting\":\"自然光影\"},\"panels\":[");

        for (int i = 0; i < panels.size(); i++) {
            VisualPanel panel = panels.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"panelNumber\":").append(panel.getPanelNumber());
            sb.append(",\"visualFocus\":\"").append(panel.getScene().substring(0, Math.min(50, panel.getScene().length()))).append("\"");
            sb.append(",\"composition\":\"").append(panel.getCameraAngle() != null ? panel.getCameraAngle() : "中景").append("\"");
            sb.append(",\"lighting\":\"自然光影\",\"mood\":\"适中\"}");
        }

        sb.append("]}");
        return sb.toString();
    }

    /**
     * 调用 DeepSeek Chat API
     */
    private String callDeepSeekChat(String userMessage, String systemMessage) {
        String apiUrl = "https://api.deepseek.com/v1/chat/completions";
        String apiKey = System.getenv("DEEPSEEK_API_KEY");

        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("DeepSeek API Key 未配置");
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("messages", List.of(
            Map.of("role", "system", "content", systemMessage),
            Map.of("role", "user", "content", userMessage)
        ));
        requestBody.put("stream", false);

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            String requestBodyStr = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(requestBodyStr, headers);

            Map<String, Object> response = restTemplate.postForObject(apiUrl, entity, Map.class);
            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
            throw new RuntimeException("DeepSeek API 返回格式异常");
        } catch (Exception e) {
            logger.error("调用 DeepSeek API 失败: {}", e.getMessage());
            throw new RuntimeException("调用 DeepSeek API 失败: " + e.getMessage());
        }
    }

    /**
     * 构建图片生成提示词
     */
    private String buildImagePrompt(String globalStyle, Map<String, Object> panelStyle, VisualPanel panel) {
        StringBuilder prompt = new StringBuilder();

        // 全局风格
        prompt.append(globalStyle).append(", ");

        // 分镜风格
        if (panelStyle != null) {
            if (panelStyle.get("visualFocus") != null) {
                prompt.append(panelStyle.get("visualFocus")).append(", ");
            }
            if (panelStyle.get("composition") != null) {
                prompt.append(panelStyle.get("composition")).append(" shot, ");
            }
            if (panelStyle.get("lighting") != null) {
                prompt.append(panelStyle.get("lighting")).append(", ");
            }
            if (panelStyle.get("mood") != null) {
                prompt.append(panelStyle.get("mood")).append(" atmosphere, ");
            }
        }

        // 分镜内容
        prompt.append(panel.getScene());
        if (panel.getCameraAngle() != null) {
            prompt.append(", ").append(panel.getCameraAngle()).append(" shot");
        }
        if (panel.getAction() != null) {
            prompt.append(", ").append(panel.getAction());
        }
        prompt.append(", manga panel, comic art, high quality");

        return prompt.toString();
    }

    /**
     * 轮询等待图片生成完成
     */
    private String pollForImage(String taskId) throws Exception {
        int maxAttempts = 150; // 5 分钟
        for (int i = 0; i < maxAttempts; i++) {
            Thread.sleep(2000);
            EvoLinkImageService.TaskStatus status = evoLinkImageService.getTaskStatus(taskId);

            if ("completed".equals(status.getStatus())) {
                return status.getImageUrl();
            } else if ("failed".equals(status.getStatus())) {
                throw new RuntimeException("图片生成失败: " + status.getError());
            }
        }
        throw new RuntimeException("图片生成超时");
    }
}

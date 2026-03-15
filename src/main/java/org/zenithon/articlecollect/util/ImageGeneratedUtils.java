package org.zenithon.articlecollect.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.volcengine.service.visual.IVisualService;
import com.volcengine.service.visual.impl.VisualServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.zenithon.articlecollect.dto.AiImageGenerationResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

/**
 * @author 1
 * @date 2026/3/15 18:05
 */
@Slf4j
public class ImageGeneratedUtils {



    /**
     * AI 生成图片
     * @param prompt AI 绘画提示词
     * @param outputPath 输出路径（相对于项目根目录）
     * @return AiImageGenerationResponse 包含成功状态、消息和图片 URL
     */
    public static AiImageGenerationResponse genearateAiImage(String prompt, String outputPath) throws Exception {
        IVisualService visualService = VisualServiceImpl.getInstance();
        visualService.setAccessKey("YOUR_VOLCENGINE_ACCESS_KEY");
        visualService.setSecretKey("YOUR_VOLCENGINE_SECRET_KEY=");
    
        JSONObject req=new JSONObject();
        //请求 Body(查看接口文档请求参数 - 请求示例，将请求参数内容复制到此)
        req.put("req_key","high_aes_general_v30l_zt2i");
        req.put("prompt", prompt);
    
        try {
            Object response = visualService.cvProcess(req);
            JSONObject jsonResponse = JSON.parseObject(JSON.toJSONString(response));
            log.info("\n=== 开始生成图片响应 ===");
            
            // 检查响应中的错误码和消息
            Integer code = jsonResponse.getInteger("code");
            String message = jsonResponse.getString("message");
            Integer status = jsonResponse.getInteger("status");
            
            // 如果有错误码且不是成功状态码（200 或 10000）
            if (code != null && code != 200 && code != 10000) {
                log.error("图片生成失败：code={}, message={}, status={}", code, message, status);
                String errorMessage = buildErrorMessage(code, message);
                return new AiImageGenerationResponse(false, errorMessage);
            }
            
            // 打印图片 URLs
            JSONObject data = jsonResponse.getJSONObject("data");
            if (data != null) {
                JSONArray base64DataArray = (JSONArray)data.get("binary_data_base64");
                String base64DataStr = (String)base64DataArray.get(0);
                if (base64DataStr != null) {
                    // 生成唯一文件名
                    String fileName = UUID.randomUUID().toString() + ".png";
                    String fullPath = Paths.get(outputPath, fileName).toString();
                    saveBase64Image(base64DataStr, fullPath);
                        
                    // 构建相对 URL（替换反斜杠为正斜杠）
                    String relativeUrl = fullPath.replace("\\", "/").replace("../", "");
                    if (!relativeUrl.startsWith("/")) {
                        relativeUrl = "/" + relativeUrl;
                    }
                        
                    log.info("\nBase64 图片已保存到：{}", fullPath);
                    log.info("图片相对 URL: {}", relativeUrl);
                    return new AiImageGenerationResponse(true, "图片生成成功", relativeUrl);
                } else {
                    log.info("\n没有 Base64 图片数据（binary_data_base64 为空）");
                    return new AiImageGenerationResponse(false, "未获取到图片数据");
                }
            }
            log.info("\n=== 完整响应 ===");
            return new AiImageGenerationResponse(false, "响应数据异常");
        } catch (Exception e) {
            log.error("调用视觉服务失败", e);
            return new AiImageGenerationResponse(false, "生成失败：" + e.getMessage());
        }
    }

    /**
     * 根据错误码和消息构建友好的错误信息
     */
    private static String buildErrorMessage(Integer code, String message) {
        if (message == null || message.isEmpty()) {
            return "图片生成失败（错误码：" + code + ")";
        }
        
        // 根据 message 内容返回对应的错误提示
        if ("Text Risk Not Pass".equals(message)) {
            return "输入文本包含敏感内容，审核未通过，请修改提示词后重试";
        } else if ("Pre Img Risk Not Pass".equals(message)) {
            return "输入图片审核未通过，可能包含敏感内容";
        } else if ("Post Img Risk Not Pass".equals(message)) {
            return "生成图片审核未通过，请重新尝试";
        } else if ("Post Text Risk Not Pass".equals(message)) {
            return "输出文本包含敏感词或版权词，审核未通过";
        } else if (message.contains("Risk Not Pass")) {
            return "内容审核未通过：" + message;
        } else if ("Request Has Reached API Limit".equals(message) || "Please Try Later".equals(message)) {
            return "API 请求超限，请稍后重试";
        } else if ("Request Has Reached API Concurrent Limit".equals(message)) {
            return "并发请求超限，请稍后重试";
        } else if ("Internal Error".equals(message) || "Internal RPC Error".equals(message)) {
            return "服务器内部错误，请稍后重试";
        } else if ("Risk Internal Error".equals(message)) {
            return "审核服务异常，请稍后重试";
        } else if ("Image Copyright Internal Error".equals(message)) {
            return "版权图服务异常，请稍后重试";
        } else {
            // 其他错误直接返回原始消息
            return message + "（错误码：" + code + ")";
        }
    }





    /**
     * 保存 Base64 图片到本地文件
     */
    private static void saveBase64Image(String base64Data, String outputPath) throws Exception {
        // 创建输出目录
        File outputFile = new File(outputPath);
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // 解码 Base64 数据
        byte[] imageBytes = Base64.getDecoder().decode(base64Data);

        // 保存到文件
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(imageBytes);
            fos.flush();
        }

        log.info("图片大小：{} 字节", imageBytes.length);
    }


}

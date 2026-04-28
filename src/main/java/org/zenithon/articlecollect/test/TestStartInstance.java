package org.zenithon.articlecollect.test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.volcengine.ApiClient;
import com.volcengine.ApiException;
import com.volcengine.service.visual.IVisualService;
import com.volcengine.service.visual.impl.VisualServiceImpl;
import com.volcengine.sign.Credentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

/**
 * @author 1
 * @date 2026/3/15 17:44
 */
public class TestStartInstance {
    private static final Logger log = LoggerFactory.getLogger(TestStartInstance.class);
    
    public static void main(String[] args) throws Exception {
        IVisualService visualService = VisualServiceImpl.getInstance();
        // call below method if you dont set ak and sk in ～/.vcloud/config
        // Use environment variables: VOLCENGINE_ACCESS_KEY and VOLCENGINE_SECRET_KEY
        String accessKey = System.getenv("VOLCENGINE_ACCESS_KEY");
        String secretKey = System.getenv("VOLCENGINE_SECRET_KEY");
        if (accessKey == null || secretKey == null) {
            log.error("Please set VOLCENGINE_ACCESS_KEY and VOLCENGINE_SECRET_KEY environment variables");
            return;
        }
        visualService.setAccessKey(accessKey);
        visualService.setSecretKey(secretKey);

        JSONObject req=new JSONObject();

        String description = "[ {\n" +
                "\n" +
                "  \"id\" : \"char-001\",\n" +
                "\n" +
                "  \"name\" : \"冷月漪\",\n" +
                "\n" +
                "  \"alternativeNames\" : [ \"冷大人\", \"月漪仙子\" ],\n" +
                "\n" +
                "  \"age\" : 29,\n" +
                "\n" +
                "  \"gender\" : \"女\",\n" +
                "\n" +
                "  \"occupation\" : \"大理寺少卿，高阶灵蕴修习者（主修契印术与媚术）\",\n" +
                "\n" +
                "  \"appearance\" : {\n" +
                "\n" +
                "    \"height\" : \"约一米七三\",\n" +
                "\n" +
                "    \"hair\" : \"如瀑黑发，常以赤玉簪高绾\",\n" +
                "\n" +
                "    \"eyes\" : \"丹凤眼，瞳色深紫，目光锐利而幽深\",\n" +
                "\n" +
                "    \"build\" : \"丰腴窈窕，曲线极具压迫性的美感\",\n" +
                "\n" +
                "    \"distinguishingFeatures\" : \"左眼角下方有一颗泪痣，十指纤长，喜戴数枚雕工繁复的赤玉指环。身着玄色官服时威严摄人，私服则偏爱深红薄纱与皮革搭配，毫不掩饰其侵略性。\"\n" +
                "\n" +
                "  },\n";

        String prompt = "你是一位专业的 AI 绘画提示词工程师。请根据以下角色信息，生成一段用于 AI 人物写真的详细描述。\n\n" +
                "要求：\n" +
                "1. **画风风格**：写实主义、高品质人物摄影、电影感光影、精细细节\n" +
                "2. **构图要求**：全身照，完整展现人物从头到脚的整体形象\n" +
                "3. **姿势造型**：根据角色性格和背景，设计自然且富有表现力的 pose 和造型（如站立、坐姿、动态姿势等）\n" +
                "4. **视角灵活**：可根据角色特点选择最佳视角（平视、仰视、俯视、侧身等）\n" +
                "5. **重点描述**：\n" +
                "   - 外貌特征：发型、发色、瞳色、面部表情、五官细节\n" +
                "   - 身材体型：身高、体态、比例\n" +
                "   - 服装搭配：服饰风格、颜色、配饰细节\n" +
                "   - 动作姿态：手部动作、身体语言、重心分布\n" +
                "6. **补充元素**：可适当添加符合角色气质的场景、道具或氛围\n" +
                "7. **输出格式**：只输出描述文本，不要其他说明，长度控制在 150-300 字\n\n" +
                "角色信息：\n" + description;


        //请求Body(查看接口文档请求参数-请求示例，将请求参数内容复制到此)
        req.put("req_key","high_aes_general_v30l_zt2i");
        req.put("prompt",prompt);

        try {
            Object response = visualService.cvProcess(req);
            JSONObject jsonResponse = (JSONObject) JSON.parseObject(JSON.toJSONString(response));
            log.info("\n=== 开始响应 ===");
            // 打印图片 URLs
            JSONObject data = jsonResponse.getJSONObject("data");
            if (data != null) {
                JSONArray base64DataArray = (JSONArray)data.get("binary_data_base64");
                String base64DataStr = (String)base64DataArray.get(0);
                if (base64DataStr != null) {
                    saveBase64Image(base64DataStr, "outputs/generated_image.png");
                    log.info("\nBase64 图片已保存到：outputs/generated_image.png");
                } else {
                    log.info("\n没有 Base64 图片数据（binary_data_base64 为空）");
                }
            }
            log.info("\n=== 完整响应 ===");
            JSONArray imageUrls = data.getJSONArray("image_urls");
            if (imageUrls != null && !imageUrls.isEmpty()) {
                log.info("=== 生成的图片 URL ===");
                for (int i = 0; i < imageUrls.size(); i++) {
                    String imageUrl = imageUrls.getString(i);
                    log.info("图片 {}: {}", (i + 1), imageUrl);
                }
            }
        } catch (Exception e) {
            log.error("调用视觉服务失败", e);
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
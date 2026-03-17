package org.zenithon.articlecollect.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.zenithon.articlecollect.util.EvoLinkImageUtil;

/**
 * EvoLink 工具类初始化器
 */
@Component
public class EvoLinkInitializer implements CommandLineRunner {
    
    @Autowired
    private EvoLinkConfig evoLinkConfig;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    public void run(String... args) throws Exception {
        // 初始化工具类
        EvoLinkImageUtil.init(
            restTemplate,
            objectMapper,
            evoLinkConfig.getApiToken(),
            evoLinkConfig.getApiUrl(),
            evoLinkConfig.getModel()
        );
    }
}

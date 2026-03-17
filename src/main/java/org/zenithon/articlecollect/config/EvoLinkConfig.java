package org.zenithon.articlecollect.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * EvoLink API 配置类
 */
@Component
public class EvoLinkConfig {
    
    @Value("${evolink.api.token:}")
    private String apiToken;
    
    @Value("${evolink.api.url:https://api.evolink.ai/v1/images/generations}")
    private String apiUrl;
    
    @Value("${evolink.api.model:z-image-turbo}")
    private String model;
    
    public String getApiToken() {
        return apiToken;
    }
    
    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }
    
    public String getApiUrl() {
        return apiUrl;
    }
    
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }
    
    public String getModel() {
        return model;
    }
    
    public void setModel(String model) {
        this.model = model;
    }
}

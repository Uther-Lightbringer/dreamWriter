package org.zenithon.articlecollect.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 火山引擎文生图配置类
 */
@Component
public class VolcEngineConfig {
    
    @Value("${volcengine.access.key:}")
    private String accessKey;
    
    @Value("${volcengine.secret.key:}")
    private String secretKey;
    
    @Value("${volcengine.image.host}")
    private String host;
    
    @Value("${volcengine.image.region}")
    private String region;
    
    @Value("${volcengine.image.service}")
    private String service;
    
    public String getAccessKey() {
        return accessKey;
    }
    
    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }
    
    public String getSecretKey() {
        return secretKey;
    }
    
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public String getRegion() {
        return region;
    }
    
    public void setRegion(String region) {
        this.region = region;
    }
    
    public String getService() {
        return service;
    }
    
    public void setService(String service) {
        this.service = service;
    }
}

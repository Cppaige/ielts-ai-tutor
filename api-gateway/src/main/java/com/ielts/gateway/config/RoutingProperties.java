package com.ielts.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "routing")
public class RoutingProperties {

    private String dataService;
    private String writingService;
    private String speakingService;

    public String getDataService() { return dataService; }
    public void setDataService(String dataService) { this.dataService = dataService; }
    public String getWritingService() { return writingService; }
    public void setWritingService(String writingService) { this.writingService = writingService; }
    public String getSpeakingService() { return speakingService; }
    public void setSpeakingService(String speakingService) { this.speakingService = speakingService; }
}

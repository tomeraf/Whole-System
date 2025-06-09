package com.halilovindustries.backend.Service.init;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "external")
public class ExternalConfig {
    private String externalUrl;

    public String getExternalUrl() {
        return externalUrl;

    }
    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }
}
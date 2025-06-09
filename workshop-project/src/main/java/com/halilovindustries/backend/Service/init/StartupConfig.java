package com.halilovindustries.backend.Service.init;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "startup")
public class StartupConfig {
    private String initFile;

    public String getInitFile() {
        return initFile;
    }
    public void setInitFile(String initFile) {
        this.initFile = initFile;
    }
}
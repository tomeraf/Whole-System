package com.halilovindustries.backend.Domain.init;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "startup")
public class StartupConfig {
    @Value("${init.file:initTest.txt}")
    private String initFile;
    private DefaultSystemManager defaultSystemManager;

    public String getInitFile() {
        return initFile;
    }
    public void setInitFile(String initFile) {
        this.initFile = initFile;
    }

    public DefaultSystemManager getDefaultSystemManager() {
        return defaultSystemManager;
    }

    public void setDefaultSystemManager(DefaultSystemManager defaultSystemManager) {
        this.defaultSystemManager = defaultSystemManager;
    }

    public static class DefaultSystemManager {
        private String name;
        private String password;
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
    }
}
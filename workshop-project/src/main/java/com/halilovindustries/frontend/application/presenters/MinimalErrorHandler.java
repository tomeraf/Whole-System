package com.halilovindustries.frontend.application.presenters;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.ErrorHandler;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@Component
public class MinimalErrorHandler implements ErrorHandler {
    private static final Logger logger = Logger.getLogger(MinimalErrorHandler.class.getName());
    private final Map<String, Long> lastErrorTime = new ConcurrentHashMap<>();
    
    @Override
    public void error(ErrorEvent event) {
        Throwable throwable = event.getThrowable();
        
        // Only log errors once every 30 seconds to avoid flooding
        String errorKey = throwable.getClass().getName() + ":" + throwable.getMessage();
        long now = System.currentTimeMillis();
        Long lastTime = lastErrorTime.get(errorKey);
        
        if (lastTime == null || (now - lastTime > 30000)) {
            lastErrorTime.put(errorKey, now);
            logger.warning("UI Error: " + throwable.getMessage());
        }
        
        // Handle corrupted tree errors by refreshing the page
        if (throwable instanceof IllegalStateException && 
            throwable.getMessage() != null && 
            throwable.getMessage().contains("tree is most likely corrupted")) {
            
            try {
                UI ui = UI.getCurrent();
                if (ui != null) {
                    // Force page reload to clear corrupted state
                    Page page = ui.getPage();
                    page.executeJs("window.location.href = '/';");
                    
                    // Also try to invalidate the session
                    if (VaadinSession.getCurrent() != null) {
                        VaadinSession.getCurrent().getSession().invalidate();
                    }
                }
            } catch (Exception e) {
                logger.warning("Failed to handle tree corruption: " + e.getMessage());
            }
        }
    }
}
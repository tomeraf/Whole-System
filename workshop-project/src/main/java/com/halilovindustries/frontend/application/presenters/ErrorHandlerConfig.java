package com.halilovindustries.frontend.application.presenters;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ErrorHandlerConfig implements VaadinServiceInitListener {
    
    private final MinimalErrorHandler errorHandler;
    private final DatabaseNavigationGuard navigationGuard;
    
    @Autowired
    public ErrorHandlerConfig(MinimalErrorHandler errorHandler, DatabaseNavigationGuard navigationGuard) {
        this.errorHandler = errorHandler;
        this.navigationGuard = navigationGuard;
    }
    
    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addSessionInitListener(initEvent -> 
            initEvent.getSession().setErrorHandler(errorHandler)
        );
        
        // Register the navigation guard
        event.getSource().addUIInitListener(uiEvent -> 
            uiEvent.getUI().addBeforeEnterListener(navigationGuard)
        );
    }
}
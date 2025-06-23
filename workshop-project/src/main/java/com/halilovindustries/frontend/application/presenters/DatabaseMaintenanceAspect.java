package com.halilovindustries.frontend.application.presenters;

import com.halilovindustries.backend.Domain.Adapters_and_Interfaces.MaintenanceModeException;
import com.halilovindustries.backend.Service.DatabaseHealthService;

import java.util.List;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;

@Aspect
@Component
public class DatabaseMaintenanceAspect {

    private final DatabaseHealthService databaseHealthService;
    
    @Autowired
    public DatabaseMaintenanceAspect(DatabaseHealthService databaseHealthService) {
        this.databaseHealthService = databaseHealthService;
    }
    
    @Around("execution(* com.halilovindustries.frontend.application.presenters.*.*(..)) && !execution(* com.halilovindustries.frontend.application.presenters.DatabaseMaintenanceAspect.*(..))")
    public Object handleMaintenanceMode(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            // Check database health if not in the middle of a maintenance mode notification
            if (!isSessionTokenMethod(joinPoint) && !isMaintenanceRelatedMethod(joinPoint) && 
                !isNavigationMethod(joinPoint)) {
                databaseHealthService.checkBeforeAction("perform this operation");
            }
            
            // Always proceed with navigation events even in maintenance mode
            if (isNavigationMethod(joinPoint)) {
                return joinPoint.proceed();
            }
            
            // Proceed with the original method call
            return joinPoint.proceed();
        } catch (MaintenanceModeException e) {
            // Handle maintenance mode exception
            UI ui = UI.getCurrent();
            if (ui != null) {
                ui.access(() -> {
                    Notification.show("System is in maintenance mode: " + e.getMessage(), 
                                    3000, Position.MIDDLE);
                });
            }
            
            // Handle common return types
            Object[] args = joinPoint.getArgs();
            if (args.length > 0 && args[args.length-1] instanceof java.util.function.Consumer) {
                // Handle consumer callbacks
                handleConsumerCallback(args[args.length-1]);
                return null;
            }
            
            return getNullReturnValue(joinPoint);
        }
    }

    // Add this new method to check if a method is navigation-related
    private boolean isNavigationMethod(ProceedingJoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        return methodName.equals("navigateTo") || 
            methodName.equals("getRouter") || 
            methodName.equals("afterNavigation") ||
            methodName.equals("beforeNavigation") ||
            methodName.equals("validateToken");
    }
    
    private boolean isSessionTokenMethod(ProceedingJoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        return methodName.equals("getSessionToken") || methodName.equals("saveSessionToken");
    }
    
    private boolean isMaintenanceRelatedMethod(ProceedingJoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        return methodName.contains("maintenance") || methodName.contains("health");
    }
    
    @SuppressWarnings("unchecked")
    private void handleConsumerCallback(Object consumer) {
        if (consumer instanceof java.util.function.Consumer) {
            try {
                // For Consumer<Boolean>
                if (consumer.getClass().toString().contains("Boolean")) {
                    ((java.util.function.Consumer<Boolean>) consumer).accept(false);
                } 
                // For Consumer<List>
                else if (consumer.getClass().toString().contains("List")) {
                    ((java.util.function.Consumer<java.util.List<?>>) consumer).accept(java.util.Collections.emptyList());
                }
                // Other consumers can be handled as needed
            } catch (Exception e) {
                // Fallback - ignore errors in callback handling
            }
        }
    }
    
    private Object getNullReturnValue(ProceedingJoinPoint joinPoint) {
        // Get the return type from the method signature
        Class<?> returnType = ((MethodSignature) joinPoint.getSignature()).getReturnType();
        
        // Handle primitive types specifically
        if (returnType == boolean.class) {
            return Boolean.FALSE;
        } else if (returnType == int.class) {
            return 0;
        } else if (returnType == long.class) {
            return 0L;
        } else if (returnType == double.class) {
            return 0.0;
        } else if (returnType == float.class) {
            return 0.0f;
        } else if (returnType == byte.class) {
            return (byte) 0;
        } else if (returnType == char.class) {
            return (char) 0;
        } else if (returnType == short.class) {
            return (short) 0;
        }
        
        // Handle common object types
        if (returnType == Boolean.class) {
            return Boolean.FALSE;
        } else if (returnType == String.class) {
            return "";
        } else if (List.class.isAssignableFrom(returnType)) {
            return java.util.Collections.emptyList();
        } else if (Map.class.isAssignableFrom(returnType)) {
            return java.util.Collections.emptyMap();
        }
        
        // Default for other object types
        return null;
    }
}
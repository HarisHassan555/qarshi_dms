package com.bezkoder.spring.login.backgroundjob;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class BackgroundJobHandlerRegistry {

    private final Map<String, BackgroundJobHandler> handlersByType = new HashMap<>();

    @Autowired
    public BackgroundJobHandlerRegistry(List<BackgroundJobHandler> handlers) {
        for (BackgroundJobHandler handler : handlers) {
            handlersByType.put(handler.jobType(), handler);
        }
    }

    public BackgroundJobHandler getHandler(String jobType) {
        return handlersByType.get(jobType);
    }
}

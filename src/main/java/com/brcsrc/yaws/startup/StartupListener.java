package com.brcsrc.yaws.startup;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupListener {
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        System.out.println("Application is ready, startup task executed");
    }
}

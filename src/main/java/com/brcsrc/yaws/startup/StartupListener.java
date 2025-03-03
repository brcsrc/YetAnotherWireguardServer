package com.brcsrc.yaws.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupListener {
    private StartupTasks startupTasks;
    private static final Logger logger = LoggerFactory.getLogger(StartupListener.class);

    @Autowired
    public StartupListener(StartupTasks startupTasks) {
        this.startupTasks = startupTasks;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        logger.info("Application is ready, executing start up tasks");
        this.startupTasks.restartActiveNetworks();
    }
}

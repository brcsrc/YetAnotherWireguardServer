package com.brcsrc.yaws;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Configuration to serve the Single Page Application.
 * Serves static files when they exist, otherwise falls back to index.html
 * to support client-side routing.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);

                        // Serve actual files if they exist (JS, CSS, images, etc.)
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }

                        // Otherwise return index.html for client-side routing
                        return new ClassPathResource("/static/index.html");
                    }
                });
    }
}

package com.api.monitor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Single static resource chain for {@code classpath:/static} with {@link SpaResourceResolver}
 * (SPA fallback to {@code index.html}). Default Spring Boot resource mappings are disabled via
 * {@code spring.web.resources.add-mappings=false} so we do not register a second conflicting {@code /**} handler.
 */
@Configuration
public class SpaWebMvcConfigurer implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new SpaResourceResolver());
    }
}

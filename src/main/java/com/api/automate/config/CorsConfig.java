package com.api.automate.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Allow CORS for your frontend
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:8080","https://car-rental-webapp.bubbleapps.io/","https://instantly-ruling-bream.ngrok-free.app") // Frontend origin
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");

    }
}

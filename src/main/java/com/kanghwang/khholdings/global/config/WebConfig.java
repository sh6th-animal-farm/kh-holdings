package com.kanghwang.khholdings.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Legacy 프로젝트의 도메인에서 API를 호출할 수 있도록 CORS를 설정
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:8888", "http://59.11.201.30:8080")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}

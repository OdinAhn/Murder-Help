package org.example.murderhelp.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
public class WebConfig {

    // page=1을 첫 페이지로 처리(기존 API 동작 유지), size 최대값을 100으로 제한
    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
        return resolver -> {
            resolver.setOneIndexedParameters(true);
            resolver.setMaxPageSize(100);
        };
    }
}

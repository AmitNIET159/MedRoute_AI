package com.medroute.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import com.medroute.security.AuthorizationInterceptor;
import com.medroute.security.RateLimitInterceptor;
import com.medroute.security.SessionInterceptor;

import java.util.List;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = {"com.medroute.controller", "com.medroute.api"})
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public InternalResourceViewResolver viewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/WEB-INF/views/");
        resolver.setSuffix(".jsp");
        return resolver;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**").addResourceLocations("/assets/");
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Rate limiter on auth endpoints
        registry.addInterceptor(new RateLimitInterceptor())
            .addPathPatterns("/auth/**");
        
        // Session check on all protected routes
        registry.addInterceptor(sessionInterceptor())
            .addPathPatterns("/**")
            .excludePathPatterns(
                "/auth/**", "/assets/**", "/error", "/favicon.ico",
                "/WEB-INF/**"
            );
        
        // Role-based authorization
        registry.addInterceptor(authorizationInterceptor())
            .addPathPatterns("/**")
            .excludePathPatterns(
                "/auth/**", "/assets/**", "/error", "/favicon.ico"
            );
    }

    @Bean
    public SessionInterceptor sessionInterceptor() { return new SessionInterceptor(); }

    @Bean
    public AuthorizationInterceptor authorizationInterceptor() { return new AuthorizationInterceptor(); }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}


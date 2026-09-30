package com.medroute.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.medroute")
public class AppConfig {

    @Bean
    public Dotenv dotenv() {
        return Dotenv.configure()
                .directory("c:/project/MedRoute_AI")
                .ignoreIfMissing()
                .load();
    }

    @Bean
    public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper();
    }
}

package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

/**
 * Debug-friendly CORS config.
 * - Uses allowedOriginPatterns to be tolerant in dev.
 * - Logs the configured frontend url to spring startup logs.
 */
@Configuration
public class CorsConfig {

    @Value("${frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        System.out.println("CorsConfig: frontend.url = " + frontendUrl);

        CorsConfiguration config = new CorsConfiguration();

        // Use allowedOriginPatterns to handle variety of dev hostnames.
        // For production lock this to exact origins (don't use "*").
        config.setAllowedOriginPatterns(List.of(frontendUrl, "*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Set-Cookie"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

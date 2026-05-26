package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    // This bean is picked up by Spring Security's filter chain.
    // CORS is processed BEFORE Spring Security rejects
    // the OPTIONS preflight — which is the actual root cause of LF-201.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Split comma-separated origins from properties file
        // so dev/staging/prod each work without code changes
        List<String> origins = List.of(allowedOrigins.split(","));
        config.setAllowedOrigins(origins);

        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With"));

        config.setAllowCredentials(true);

        // How long the browser can cache preflight response (1 hour)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // Explicit CorsFilter bean ensures CORS runs early in the
    // filter chain — before Spring Security's authentication filters.
    // This is what makes OPTIONS preflight pass without auth headers.
    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }
}
package com.api.monitor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http)
            throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public pages
                .requestMatchers(
                    "/",
                    "/css/**",
                    "/js/**",
                    "/actuator/health",
                    "/debug-env",
                    "/debug-env1"
                ).permitAll()
                // Everything else needs login
                .anyRequest().authenticated()
            )
            // Google SSO
            .oauth2Login(oauth -> oauth
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/?error=true")
            )
            // Logout
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .permitAll()
            );

        return http.build();
    }
}
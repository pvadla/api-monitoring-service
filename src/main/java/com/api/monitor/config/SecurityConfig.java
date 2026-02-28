package com.api.monitor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework
    .security.web.SecurityFilterChain;

import com.api
    .monitor.service.CustomOAuth2UserService;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService
        customOAuth2UserService;

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http) throws Exception {
        http
            // Disable CSRF
            .csrf(AbstractHttpConfigurer::disable)

            // Allow H2 frames
            .headers(headers -> headers
                .frameOptions(frame -> frame.disable())
            )

            .authorizeHttpRequests(auth -> auth
                // ✅ Simple string matchers - no import needed
                .requestMatchers(
                    "/",
                    "/css/**",
                    "/js/**",
                    "/actuator/**",
                    "/debug-env",
                    "/h2-console",
                    "/h2-console/**",
                    "/h2-console/login.do/**",
                    "/status/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth -> oauth
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/?error=true")
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .permitAll()
            );

        return http.build();
    }
}
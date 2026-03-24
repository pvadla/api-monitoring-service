package com.api.monitor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation
    .web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;

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

            // SPA fetch() to /api/** must get 401 JSON, not a 302 to OAuth (breaks apiClient).
            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    apiJsonUnauthorizedMatcher()
                )
            )

            .authorizeHttpRequests(auth -> auth
                // Public SPA routes (explicit GET: string "/" alone is unreliable with some PathPattern setups)
                .requestMatchers(
                    HttpMethod.GET,
                    "/",
                    "/index.html",
                    "/about",
                    "/contact",
                    "/login"
                ).permitAll()
                .requestMatchers(
                    "/css/**",
                    "/js/**",
                    "/assets/**",
                    "/favicon.svg",
                    "/favicon.ico",
                    "/actuator/**",
                    "/debug-env",
                    "/debug-env1",
                    "/h2-console",
                    "/h2-console/**",
                    "/h2-console/login.do/**",
                    "/status/**",
                    "/contact",
                    "/about",
                    "/heartbeat/**",
                    "/webhooks/**",
                    "/api/public/**",
                    "/oauth2/**",
                    "/login",
                    "/login/**"
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

    /**
     * Matches {@code /api/**} (after servlet context path). Used instead of {@code AntPathRequestMatcher},
     * which is not on the classpath in some Spring Security 7 / modular setups.
     */
    private static RequestMatcher apiJsonUnauthorizedMatcher() {
        return request -> {
            String uri = request.getRequestURI();
            if (uri == null) {
                return false;
            }
            String contextPath = request.getContextPath();
            if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
                uri = uri.substring(contextPath.length());
            }
            return uri.startsWith("/api/");
        };
    }
}
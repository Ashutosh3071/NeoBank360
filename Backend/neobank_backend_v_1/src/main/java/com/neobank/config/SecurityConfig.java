package com.neobank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth

                // ── Preflight ──
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // ── Public endpoints (no token required) ──
                .requestMatchers(
                    "/api/application/send-otp",
                    "/api/application/submit",
                    "/api/application/status/send-otp",
                    "/api/application/status/verify",
                    "/api/auth/**",
                    "/actuator/health"
                ).permitAll()

                // ── Customer + Admin ──
                .requestMatchers(
                    "/api/upi/**",
                    "/api/budgets/**",
                    "/api/bills/**",
                    "/api/rewards/**",
                    "/api/account-request/submit",
                    "/api/account-request/my",
                    "/api/application/submit-auth"
                ).hasAnyRole("CUSTOMER", "ADMIN")

                // ── Authenticated (any valid token) ──
                .requestMatchers(
                    "/api/auth/change-password",
                    "/api/account/my",
                    "/api/transaction/transfer",
                    "/api/transaction/my/**",
                    "/api/dev/**"
                ).authenticated()

                // ── Admin / Super Admin / Manager ──
                .requestMatchers(
                    "/api/transaction/admin/**",
                    "/api/account/admin/**",
                    "/api/account-request/admin/**"
                ).hasAnyRole("ADMIN", "SUPER_ADMIN", "MANAGER")

                // ── Admin / Super Admin only ──
                .requestMatchers(
                    "/api/application/all",
                    "/api/application/approve",
                    "/api/application/**"
                ).hasAnyRole("ADMIN", "SUPER_ADMIN")

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "http://localhost:4201",
            "http://localhost:4200",
            "http://localhost:3000",
            "https://your-frontend.com"
        ));
        config.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        config.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With"
        ));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
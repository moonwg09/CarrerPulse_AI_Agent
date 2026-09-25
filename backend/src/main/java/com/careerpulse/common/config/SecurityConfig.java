package com.careerpulse.common.config;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/api/v1/auth/signup"
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // 오류 처리 과정 허용
                        .dispatcherTypeMatchers(
                                DispatcherType.ERROR
                        ).permitAll()

                        // 회원가입 허용
                        .requestMatchers(
                                "/api/v1/auth/signup",
                                "/error"
                        ).permitAll()

                        // 나머지는 로그인 필요
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
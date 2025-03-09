package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

@Configuration
@EnableWebSecurity
public class SecurityConfig { 
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorizeRequests -> 
                    authorizeRequests
                        .requestMatchers("/contacts").authenticated() 
                        .anyRequest().permitAll()) 
                .oauth2Login(oauth2 -> oauth2
                    .defaultSuccessUrl("/contacts", true))
                .logout(logout -> logout
                    .logoutSuccessUrl("/"))
                .csrf(AbstractHttpConfigurer::disable) 
                .build();
    }
}

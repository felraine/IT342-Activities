package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
                                .requestMatchers("/contacts").authenticated()  // Only authenticated users can access /contacts
                                .anyRequest().permitAll()  // Permit all other requests
                )
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/contacts", true))  // Redirect to /contacts after successful login
                .logout(logout -> logout
                        .logoutSuccessUrl("/"))  // Redirect to root after logout
                .formLogin(form -> form
                        .loginPage("/login")  // Custom login page URL
                        .permitAll())  // Allow everyone to access the login page
                .csrf(AbstractHttpConfigurer::disable)  // Disable CSRF protection for simplicity
                .build();
    }
}

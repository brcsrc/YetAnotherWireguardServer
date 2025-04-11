package com.brcsrc.yaws.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    public SecurityConfiguration(UserDetailsServiceImpl userDetailsService, JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity.authorizeHttpRequests(registry -> {

            // Allow unauthenticated access to frontend resources
            registry.requestMatchers(
                    "/", // Allow access to the root path
                    "/index.html", // Allow access to the main frontend file
                    "/static/**", // Allow access to static resources
                    "/assets/**", // Allow access to assets (CSS, JS, images)
                    "/vite.svg" // Allow access to the Vite logo
            ).permitAll();

            // Allow unauthenticated access to public API endpoints
            registry.requestMatchers(
                    "/api/v1/user/register", // User creation API
                    "/api/v1/user/authenticate", // User authentication API
                    "/error" // Allow error page access
            ).permitAll();

            // Allow Swagger and OpenAPI docs in development mode
            boolean isDev = Boolean.parseBoolean(System.getenv("DEV"));
            if (isDev) {
                registry.requestMatchers(
                        "/swagger-ui/**", // Swagger UI static resources
                        "/swagger-ui.html", // Swagger UI main page
                        "/v3/api-docs/**", // OpenAPI docs
                        "/api-docs/**" // Alternative path
                ).permitAll();
            }

            // Require authentication for all other requests
            registry.anyRequest().authenticated();
        })
                .formLogin().disable()
                .csrf().disable()
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setUserDetailsService(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
        return daoAuthenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(authenticationProvider());
    }
}

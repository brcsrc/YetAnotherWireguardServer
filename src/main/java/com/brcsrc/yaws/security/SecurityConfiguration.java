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

            // these are unauthenticated endpoints that must be public
            registry.requestMatchers(
            "/api/v1/user/register",       // user creation api
                "/api/v1/user/authenticate",        // user authentication api
                "/error"                            // without this, any client exception thrown is thrown as a 403 instead
            ).permitAll();

            // these are extra endpoints for showing swagger and openapi docs
            boolean isDev = Boolean.parseBoolean(System.getenv("DEV"));
            if (isDev) {
                registry.requestMatchers(
                        "/swagger-ui/**",  // Swagger UI static resources
                        "/swagger-ui.html",         // Swagger UI main page
                        "/v3/api-docs/**",          // OpenAPI docs
                        "/api-docs/**"              // Alternative path
                ).permitAll();
            }

            // any other requests to these endpoints would require authentication:
            //      "/api/v1/networks/**"
            //      "/api/v1/clients/**"
            //      "/api/v1/user/update"

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




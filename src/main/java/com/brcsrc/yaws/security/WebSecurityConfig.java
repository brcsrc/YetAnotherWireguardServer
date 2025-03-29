package com.brcsrc.yaws.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public UserDetailsService userDetailsService() {
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
        manager.createUser(User.withDefaultPasswordEncoder().username("user").password("password").roles("USER").build());
        return manager;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .requestMatchers(
                        "/", // Allow access to the root path
                        "/index.html", // Allow access to the main frontend file
                        "/static/**", // Allow access to static resources
                        "/assets/**", // Allow access to assets (CSS, JS, images)
                        "/api/v1/networks/**", // Allow access to API endpoints
                        "/swagger-ui/**", // Allow access to Swagger UI
                        "/v3/api-docs/**" // Allow access to OpenAPI docs
                ).permitAll() // Permit access to these paths
                .anyRequest().authenticated() // Protect all other routes
                .and()
                .formLogin().disable() // Disable the default login page
                .csrf().disable();     // Disable CSRF for now (can be re-enabled later)
        return http.build();
    }
}

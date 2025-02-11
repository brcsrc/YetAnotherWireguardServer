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
                        "/login",
                        "/register",
                        "/api/v1/networks/**",
                        "/api/v1/clients/**",
                        "/swagger-ui/**",       // Swagger UI static resources
                        "/swagger-ui.html",     // Swagger UI main page
                        "/v3/api-docs/**",      // OpenAPI docs
                        "/api-docs/**",        // Alternative path
                        "/error"            // without this, any client exception thrown is thrown as a 403 instead
                ).permitAll()  // these are allowed for now
                .anyRequest().authenticated()  // protects other routes
                .and()
                .formLogin().disable() // disable the default login page
                .csrf().disable();  // TODO reenable later
        return http.build();
    }
}




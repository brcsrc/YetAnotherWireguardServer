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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.ArrayList;
import java.util.List;

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
    UrlBasedCorsConfigurationSource corsConfigurationSource() {
        // TODO we need to add env vars for domains here if specified
        List<String> allowedOrigins = new ArrayList<>();

        // allow access from vite if started in dev
        boolean isDev = Boolean.parseBoolean(System.getenv("DEV"));
        if (isDev) {
            allowedOrigins.add("http://localhost:5173");
        }

        // allow methods in swagger-ui/index.html
        List<String> allowedMethods = List.of("OPTIONS", "GET", "POST", "PATCH", "DELETE");

        // allow headers set in most requests
        List<String> allowedHeaders = List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin"
        );

        // https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(allowedMethods);
        configuration.setAllowedHeaders(allowedHeaders);
        configuration.setAllowCredentials(true); // tell browser to send HttpOnly cookies
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        // the ordering for this matters https://docs.spring.io/spring-security/reference/servlet/architecture.html#servlet-security-filters
        return httpSecurity
                .cors(httpSecurityCorsConfigurer -> {httpSecurityCorsConfigurer.configurationSource(corsConfigurationSource());})
                .csrf().disable()
                .authorizeHttpRequests(registry -> {
                    // Allow unauthenticated access to frontend resources
                    registry.requestMatchers(
                            "/",    // Allow access to the root path
                            "/index.html",  // Allow access to the main frontend file
                            "/static/**",   // Allow access to static resources
                            "/assets/**",   // Allow access to assets (CSS, JS, images)
                            "/vite.svg"     // Allow access to the Vite logo
                    ).permitAll();

                    // Allow unauthenticated access to public API endpoints
                    registry.requestMatchers(
                            "/api/v1/user/register",            // User creation API
                            "/api/v1/user/authenticate",        // User authentication API
                            "/api/v1/user/logout",              // User logout API
                            "/error"                            // Allow error page access
                    ).permitAll();

                    // Allow Swagger and OpenAPI docs in development mode
                    boolean isDev = Boolean.parseBoolean(System.getenv("DEV"));
                    if (isDev) {
                        registry.requestMatchers(
                                "/swagger-ui/**",           // Swagger UI static resources
                                "/swagger-ui.html",         // Swagger UI main page
                                "/v3/api-docs/**",          // OpenAPI docs
                                "/api-docs/**"              // Alternative path
                        ).permitAll();
                    }

                    // Allow all GET requests for SPA routing (except API calls which are handled above)
                    // todo add security tests to see if this is as unsafe as it seems
                    registry.requestMatchers(request ->
                        "GET".equals(request.getMethod()) &&
                        !request.getRequestURI().toLowerCase().startsWith("/api/")
                    ).permitAll();

                    // Require authentication for all other requests
                    registry.anyRequest().authenticated();
                })
                .formLogin().disable()
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

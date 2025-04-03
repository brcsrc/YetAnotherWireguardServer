package com.brcsrc.yaws.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    @Autowired
    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsServiceImpl userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        logger.info("routing request through JwtAuthenticationFilter");
        // grab the authorization header. this header is intended to be set in requests as:
        //     '-H "Authorization Bearer <encoded JWT>"
        // if it is not set or not formatted correctly, just exit to main filter chain
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.info("request does not include Authorization header");
            filterChain.doFilter(request, response);
            return;
        }

        // the first 7 chars of the authHeader string should be 'Bearer '
        // get the jwt value after
        String jwt = authHeader.substring(7);
        String userName = this.jwtService.extractUsernameFromJwt(jwt);
        logger.info(String.format("username from jwt: %s", userName));

        if (userName != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // TODO make loadUserByUsername throw UserNotFoundException, catch here, respond with 403/401
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userName);

            if (userDetails != null && this.jwtService.isTokenValid(jwt)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userName,
                        userDetails.getPassword(),
                        userDetails.getAuthorities()  // this is needed for auth success
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                logger.info(String.format("authentication for user '%s' successful", userName));
            }
        }
        filterChain.doFilter(request, response);
    }
}

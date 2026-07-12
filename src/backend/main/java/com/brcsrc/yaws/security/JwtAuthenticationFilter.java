package com.brcsrc.yaws.security;

import com.brcsrc.yaws.utility.HeaderUtils;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

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
        // grab the authorization token from cookies
        // if it is not set or not formatted correctly, just exit to main filter chain
        String jwt = HeaderUtils.getRequestHttpOnlyAuthTokenCookieValue(request);
        if (jwt == null) {
            logger.info("request does not include Authorization token cookie");
            filterChain.doFilter(request, response);
            return;
        }

        // extractUsernameFromJwt tries to validate that the jwt was signed with the
        // secret key and throws SignatureException if invalid. in that case we respond 401
        String userName;
        try {
            userName = this.jwtService.extractUsernameFromJwt(jwt);
        } catch (SignatureException e) {
            String errMsg = String.format("jwt is invalid: %s", e.getMessage());
            logger.error(errMsg);
            // Set 401 status and clear the invalid token cookie
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader("Set-Cookie", "accessToken=; Max-Age=0; Path=/; HttpOnly; SameSite=Strict");
            return;
        }

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

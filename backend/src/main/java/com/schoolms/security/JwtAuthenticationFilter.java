package com.schoolms.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.JwtException;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestUri = request.getRequestURI();
        String auth = request.getHeader("Authorization");
        boolean hasAuthorizationHeader = auth != null && !auth.isBlank();
        log.debug("JWT filter requestUri='{}', authorizationHeaderPresent={}", requestUri, hasAuthorizationHeader);

        String token = extractBearerToken(auth);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        log.debug("JWT filter token extraction succeeded for requestUri='{}'", requestUri);
        try {
            String email = jwtService.parse(token).getSubject();
            log.debug("JWT filter extracted subject='{}' for requestUri='{}'", email, requestUri);
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
                log.debug("JWT filter loaded authorities={} for subject='{}'", userDetails.getAuthorities(), email);
                if (jwtService.isValid(token, userDetails.getUsername()) && userDetails.isEnabled()) {
                    var authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("JWT filter set SecurityContext authentication for subject='{}'", email);
                }
            }
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
            SecurityContextHolder.clearContext();
            log.debug("JWT filter rejected token for requestUri='{}': {}", requestUri, ex.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null) {
            return null;
        }
        String header = authorizationHeader.trim();
        if (header.length() < 8 || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String token = header.substring(7).trim();
        return token.isEmpty() ? null : token;
    }
}

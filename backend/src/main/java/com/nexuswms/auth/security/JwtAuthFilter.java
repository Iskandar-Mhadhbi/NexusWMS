package com.nexuswms.auth.security;

import com.nexuswms.auth.service.TokenBlocklistService;
import com.nexuswms.user.entity.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse; 
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenBlocklistService tokenBlocklistService;

    public JwtAuthFilter(JwtUtil jwtUtil, TokenBlocklistService tokenBlocklistService) {
        this.jwtUtil = jwtUtil;
        this.tokenBlocklistService = tokenBlocklistService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        Objects.requireNonNull(request, "HttpServletRequest must not be null");
        Objects.requireNonNull(response, "HttpServletResponse must not be null");
        Objects.requireNonNull(filterChain, "FilterChain must not be null");
        
        final String authHeader = request.getHeader("Authorization");
        String resolvedToken = null;

        // 1. Try extracting from Header
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            resolvedToken = authHeader.substring(7);
        } 
        // 2. Fallback to Query Parameter for WebSockets/SockJS
        else if (request.getParameter("token") != null) {
            resolvedToken = request.getParameter("token");
        }

        // 3. If no token is found anywhere, pass to the next filter (Spring Security will block it if required)
        if (resolvedToken == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 4. Validate the unified token
        if (!jwtUtil.isTokenValid(resolvedToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId = jwtUtil.extractUserId(resolvedToken);
        
        // 5. Check Redis blocklist
        if (tokenBlocklistService.isUserBlocked(userId)) {
            filterChain.doFilter(request, response);
            return;
        }

        String roleStr = jwtUtil.extractRole(resolvedToken);
        Role role;
        
        // 6. Validate role enum
        try {
            role = Role.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            filterChain.doFilter(request, response);
            return;
        }

        // 7. Authenticate
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );

        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
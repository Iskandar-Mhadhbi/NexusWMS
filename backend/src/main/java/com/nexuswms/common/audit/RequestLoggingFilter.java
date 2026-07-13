package com.nexuswms.common.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Logs every incoming HTTP request with method, path, status, duration,
 * and the authenticated actor (if present).
 *
 * <p>This is the single point of access logging for the application —
 * "every action by every worker is logged" is a core design principle
 * of NexusWMS, and this filter is what makes that true at the HTTP layer
 * regardless of which controller or service ultimately handles the request.</p>
 *
 * <p>Runs after {@code JwtAuthFilter} so the authenticated principal
 * (if any) is already available in the {@code SecurityContext} when
 * the request completes.</p>
 */
@Component
@Order(Integer.MAX_VALUE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("com.nexuswms.access");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String method = request.getMethod();
        String path = request.getRequestURI();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            int status = response.getStatus();
            String actor = resolveActor();

            log.info(
                    "{} {} -> {} ({}ms) actor={}",
                    method, path, status, durationMs, actor
            );
        }
    }

    /**
     * Resolves the authenticated user ID from the SecurityContext, if present.
     * Anonymous requests (e.g. login, register) log as "anonymous".
     *
     * @return The principal's user ID string, or "anonymous" if unauthenticated.
     */
    private String resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }
        Object principal = authentication.getPrincipal();
        return principal != null ? principal.toString() : "anonymous";
    }
}
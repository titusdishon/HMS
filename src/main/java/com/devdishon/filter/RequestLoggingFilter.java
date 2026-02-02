package com.devdishon.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that logs all incoming HTTP requests and outgoing responses.
 * Captures user information, request details, and response status for audit purposes.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger accessLogger = LoggerFactory.getLogger("ACCESS_LOG");
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private static final String CORRELATION_ID = "correlationId";
    private static final String USER_EMAIL = "userEmail";
    private static final String REQUEST_URI = "requestUri";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Skip logging for certain paths
        if (shouldSkipLogging(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Generate correlation ID for request tracking
        String correlationId = generateCorrelationId(request);

        // Wrap request and response for content caching
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            // Set MDC context for logging
            MDC.put(CORRELATION_ID, correlationId);
            MDC.put(REQUEST_URI, request.getRequestURI());

            // Add correlation ID to response header
            response.setHeader("X-Correlation-ID", correlationId);

            // Log incoming request
            logRequest(wrappedRequest, correlationId);

            // Process the request
            filterChain.doFilter(wrappedRequest, wrappedResponse);

        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // Get user info after authentication is processed
            String userEmail = getUserEmail();
            MDC.put(USER_EMAIL, userEmail);

            // Log outgoing response
            logResponse(wrappedRequest, wrappedResponse, correlationId, userEmail, duration);

            // Copy response body to actual response
            wrappedResponse.copyBodyToResponse();

            // Clear MDC context
            MDC.clear();
        }
    }

    private boolean shouldSkipLogging(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // Skip actuator health checks and static resources
        return uri.startsWith("/actuator/health") ||
               uri.startsWith("/swagger-ui") ||
               uri.startsWith("/v3/api-docs") ||
               uri.startsWith("/favicon.ico") ||
               uri.equals("/");
    }

    private String generateCorrelationId(HttpServletRequest request) {
        // Check if correlation ID is provided in header
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString().substring(0, 8);
        }
        return correlationId;
    }

    private String getUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return "anonymous";
    }

    private void logRequest(ContentCachingRequestWrapper request, String correlationId) {
        String queryString = request.getQueryString();
        String path = queryString != null ?
            request.getRequestURI() + "?" + queryString :
            request.getRequestURI();

        accessLogger.info("[{}] --> {} {} | IP: {} | User-Agent: {}",
                correlationId,
                request.getMethod(),
                path,
                getClientIp(request),
                request.getHeader("User-Agent"));
    }

    private void logResponse(ContentCachingRequestWrapper request,
                            ContentCachingResponseWrapper response,
                            String correlationId,
                            String userEmail,
                            long duration) {
        int status = response.getStatus();
        String statusCategory = getStatusCategory(status);

        accessLogger.info("[{}] <-- {} {} | Status: {} {} | User: {} | Duration: {}ms",
                correlationId,
                request.getMethod(),
                request.getRequestURI(),
                status,
                statusCategory,
                userEmail,
                duration);

        // Log additional details for errors
        if (status >= 400) {
            logger.warn("[{}] Error response: {} {} returned {} for user {}",
                    correlationId,
                    request.getMethod(),
                    request.getRequestURI(),
                    status,
                    userEmail);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }

    private String getStatusCategory(int status) {
        if (status >= 500) return "SERVER_ERROR";
        if (status >= 400) return "CLIENT_ERROR";
        if (status >= 300) return "REDIRECT";
        if (status >= 200) return "SUCCESS";
        return "INFO";
    }
}

package com.nexusai.api.security.ratelimit;

import com.nexusai.auth.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {

        String path = request.getRequestURI();

        // Skip rate limiting for health checks
        if (path.contains("/actuator") || path.contains("/health")) {
            return true;
        }

        RateLimitService.RateLimitResult result;

        // Check auth endpoints by IP
        if (path.contains("/auth/")) {
            String clientIp = getClientIp(request);
            result = rateLimitService.checkAuthRateLimit(clientIp);
        } else {
            // Check authenticated endpoints by user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
                UUID userId = principal.getUserId();

                // Check specific endpoint rate limits
                if (path.contains("/messages") || path.contains("/generate") || path.contains("/stream")) {
                    result = rateLimitService.checkMessageRateLimit(userId);
                } else if (path.contains("/upload") || path.contains("/media")) {
                    result = rateLimitService.checkUploadRateLimit(userId);
                } else {
                    result = rateLimitService.checkApiRateLimit(userId);
                }
            } else {
                // Anonymous request - use IP-based limiting
                String clientIp = getClientIp(request);
                result = rateLimitService.checkEndpointRateLimit("anonymous", clientIp, 30);
            }
        }

        if (!result.allowed()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Rate limit exceeded\",\"retryAfter\":" + result.retryAfterSeconds() + "}");
            return false;
        }

        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remainingTokens()));
        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}

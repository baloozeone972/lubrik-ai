package com.nexusai.api.security.audit;

import com.nexusai.auth.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditService auditService;

    @Around("@annotation(auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        String action = auditable.action();
        String resource = auditable.resource();

        UUID userId = getCurrentUserId();
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();

            // Log successful action
            String details = String.format("Method: %s, Duration: %dms",
                    joinPoint.getSignature().getName(),
                    System.currentTimeMillis() - startTime);

            auditService.logUserAction(userId, action, resource, details);

            return result;
        } catch (Throwable e) {
            // Log failed action
            auditService.logError(userId, action,
                    "Operation failed: " + e.getMessage(),
                    getStackTrace(e));
            throw e;
        }
    }

    @Around("execution(* com.nexusai.api.controller..*.*(..)) && !@annotation(Auditable)")
    public Object auditControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getMethod().getName();
        String className = signature.getDeclaringType().getSimpleName();

        UUID userId = getCurrentUserId();

        try {
            Object result = joinPoint.proceed();

            // Only log significant operations
            if (isSignificantOperation(methodName)) {
                auditService.logUserAction(userId, methodName, className,
                        "Controller method executed");
            }

            return result;
        } catch (Throwable e) {
            auditService.logError(userId, className + "." + methodName,
                    e.getMessage(), getStackTrace(e));
            throw e;
        }
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUserId();
        }
        return null;
    }

    private boolean isSignificantOperation(String methodName) {
        return methodName.startsWith("create") ||
               methodName.startsWith("update") ||
               methodName.startsWith("delete") ||
               methodName.startsWith("send") ||
               methodName.startsWith("upload") ||
               methodName.startsWith("generate");
    }

    private String getStackTrace(Throwable e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            if (element.getClassName().startsWith("com.nexusai")) {
                sb.append(element.toString()).append("\n");
            }
            if (sb.length() > 500) break;
        }
        return sb.toString();
    }
}

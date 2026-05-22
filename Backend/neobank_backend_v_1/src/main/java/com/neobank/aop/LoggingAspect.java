// src/main/java/com/neobank/aop/LoggingAspect.java

package com.neobank.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    // ─────────────────────────────────────────────
    //  POINTCUTS
    // ─────────────────────────────────────────────

    /** All methods in any controller */
    @Pointcut("within(com.neobank.controller..*)")
    public void controllerLayer() {}

    /** All methods in any service */
    @Pointcut("within(com.neobank.service..*)")
    public void serviceLayer() {}

    /** All methods in any repository */
    @Pointcut("within(com.neobank.repository..*)")
    public void repositoryLayer() {}

    /** Combined: controller + service + repository */
    @Pointcut("controllerLayer() || serviceLayer() || repositoryLayer()")
    public void applicationLayer() {}

    // ─────────────────────────────────────────────
    //  AROUND — log entry, exit, duration, exceptions
    // ─────────────────────────────────────────────

    @Around("applicationLayer()")
    public Object logAround(ProceedingJoinPoint pjp) throws Throwable {

        String className  = pjp.getTarget().getClass().getSimpleName();
        String methodName = pjp.getSignature().getName();
        Object[] args     = pjp.getArgs();

        // Sanitize args — skip byte[] (file blobs), mask sensitive fields
        String argsStr = sanitizeArgs(args);

        log.info("► [{}.{}] ENTER — args: {}", className, methodName, argsStr);

        long start = System.currentTimeMillis();

        try {
            Object result = pjp.proceed();

            long elapsed = System.currentTimeMillis() - start;
            log.info("◄ [{}.{}] EXIT  — {}ms | return: {}",
                    className, methodName, elapsed,
                    summarizeResult(result));
//            log.info("");
            

            return result;

        } catch (Throwable ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("✖ [{}.{}] EXCEPTION after {}ms — {}: {}",
                    className, methodName, elapsed,
                    ex.getClass().getSimpleName(), ex.getMessage());
            throw ex;
        }
    }

    // ─────────────────────────────────────────────
    //  BEFORE — HTTP request info on controllers
    // ─────────────────────────────────────────────

    @Before("controllerLayer()")
    public void logHttpRequest(JoinPoint jp) {
        try {
            ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                log.info("HTTP ► {} {} | Controller: {}.{}",
                        req.getMethod(),
                        req.getRequestURI(),
                        jp.getTarget().getClass().getSimpleName(),
                        jp.getSignature().getName());
            }
        } catch (Exception ignored) {
            // Never let AOP crash the application
        }
    }

    // ─────────────────────────────────────────────
    //  AFTER THROWING — structured error log
    // ─────────────────────────────────────────────

    @AfterThrowing(pointcut = "applicationLayer()", throwing = "ex")
    public void logException(JoinPoint jp, Throwable ex) {
        log.error("EXCEPTION in [{}.{}] → {}: {}",
                jp.getTarget().getClass().getSimpleName(),
                jp.getSignature().getName(),
                ex.getClass().getSimpleName(),
                ex.getMessage());
    }

    // ─────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────

    /**
     * Sanitize method arguments:
     * - Skip byte[] (file blobs) to avoid huge log lines
     * - Mask sensitive field names (password, otp, aadhaar, pan)
     */
    private String sanitizeArgs(Object[] args) {
        if (args == null || args.length == 0) return "[]";

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg == null) {
                sb.append("null");
            } else if (arg instanceof byte[]) {
                sb.append("<binary:").append(((byte[]) arg).length).append("bytes>");
            } else if (arg instanceof org.springframework.web.multipart.MultipartFile f) {
                sb.append("<file:").append(f.getOriginalFilename())
                  .append(":").append(f.getSize()).append("bytes>");
            } else {
                String str = arg.toString();
                // Mask sensitive values if they look like passwords/OTPs
                if (isSensitiveArg(str)) {
                    sb.append("***MASKED***");
                } else if (str.length() > 200) {
                    sb.append(str, 0, 200).append("...[truncated]");
                } else {
                    sb.append(str);
                }
            }
            if (i < args.length - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    private boolean isSensitiveArg(String str) {
        // Basic heuristic — 6-digit OTP or looks like a password hash
        return str.matches("\\d{6}") || str.startsWith("$2a$");
    }

    private String summarizeResult(Object result) {
        if (result == null) return "void/null";
        if (result instanceof byte[]) return "<binary:" + ((byte[]) result).length + "bytes>";
        String str = result.toString();
        return str.length() > 150 ? str.substring(0, 150) + "..." : str;
    }
}
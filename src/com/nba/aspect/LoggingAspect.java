package com.nba.aspect;

import com.nba.model.AuditLog;
import com.nba.repository.AuditLogRepository;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);
    private final AuditLogRepository auditLogRepository;

    public LoggingAspect(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Pointcut("execution(* com.nba.service.TeamManager.add*(..)) || " +
            "execution(* com.nba.service.TeamManager.update*(..)) || " +
            "execution(* com.nba.service.TeamManager.patch*(..)) || " +
            "execution(* com.nba.service.TeamManager.remove*(..))")
    public void loggableMethods() {}

    @Before("loggableMethods()")
    public void logBeforeChange(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        String argumentsString = Arrays.toString(args);

        logger.info("AUDIT LOG [START]: Executing operation '{}' with arguments: {}", methodName, argumentsString);
    }

    @AfterReturning("loggableMethods()")
    public void logAfterSuccess(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        String argumentsString = Arrays.toString(args);

        logger.info("AUDIT LOG [SUCCESS]: Operation '{}' completed successfully.", methodName);

        AuditLog auditLog = new AuditLog(methodName, argumentsString);
        auditLogRepository.save(auditLog);
    }
}
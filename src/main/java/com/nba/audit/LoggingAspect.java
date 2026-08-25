package com.nba.audit;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@RequiredArgsConstructor
 class LoggingAspect {


    private final AuditLogService auditLogService;
    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    @Pointcut("execution(* com.nba..*Service.add*(..)) || " +
            "execution(* com.nba..*Service.update*(..)) || " +
            "execution(* com.nba..*Service.delete*(..)) || " +
            "execution(* com.nba..*Service.remove*(..)) || " +
            "execution(* com.nba..*Service.fire*(..))")
    public void serviceModifyingMethods() {
    }


    @AfterReturning(pointcut = "serviceModifyingMethods()")
    public void logAfterReturning(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        String arguments = Arrays.toString(joinPoint.getArgs());

        logger.info("AUDIT LOG [SUCCESS]: Operation '{}' completed successfully.", methodName);


        AuditLog log = AuditLog.builder()
                .methodName(methodName)
                .arguments(arguments)
                .status(LogStatus.SUCCESS)
                .build();

        auditLogService.saveLog(log);
    }

    @AfterThrowing(pointcut = "serviceModifyingMethods()", throwing = "error")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable error) {
        String methodName = joinPoint.getSignature().getName();
        String arguments = Arrays.toString(joinPoint.getArgs());

        logger.error("AUDIT LOG [ERROR]: Operation '{}' failed. Error: {}", methodName, error.getMessage());


        AuditLog log = AuditLog.builder()
                .methodName(methodName)
                .arguments(arguments)
                .status(LogStatus.ERROR)
                .errorMessage(error.getMessage())
                .build();

        auditLogService.saveLog(log);
    }
}
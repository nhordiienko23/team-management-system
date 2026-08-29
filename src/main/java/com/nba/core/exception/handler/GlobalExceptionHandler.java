package com.nba.core.exception.handler;


import com.nba.audit.AuditLog;
import com.nba.audit.AuditLogService;
import com.nba.audit.LogStatus;
import com.nba.core.dto.response.ErrorResponse;
import com.nba.core.exception.invalidData.InvalidDataException;
import com.nba.core.exception.notFound.ResourceNotFoundException;
import com.nba.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    // Using a logger instead of ex.printStackTrace() for better log management
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final AuditLogService auditLogService;

    // --- CUSTOM EXCEPTIONS ---

    @ExceptionHandler({InvalidDataException.class})
    public ResponseEntity<ErrorResponse> handleInvalidData(InvalidDataException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler({ResourceNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {

        // 1. Узнаем, куда пытались зайти (Например: "DELETE /api/admin/users/3")
        String attemptedOperation = request.getMethod() + " " + request.getRequestURI();

        // 2. Достаем информацию о пользователе из контекста безопасности
        String userInfo = "Anonymous (Unauthenticated)";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            userInfo = String.format("User ID: %d, Username: '%s'",
                    userDetails.getUser().getId(),
                    userDetails.getUsername());
        }

        // 3. Формируем подробное сообщение для логов
        String auditMessage = String.format("Target: [%s] | By: [%s]", attemptedOperation, userInfo);
        logger.warn("SECURITY ALERT: {} - {}", ex.getMessage(), auditMessage);

        // 4. Сохраняем в базу данных
        AuditLog log = AuditLog.builder()
                .methodName("SECURITY_VIOLATION")
                .arguments(auditMessage) // Сохраняем КТО и КУДА лез
                .status(LogStatus.ERROR)
                .errorMessage(ex.getMessage())
                .build();

        auditLogService.saveLog(log);

        return buildResponse(HttpStatus.FORBIDDEN, "Forbidden", "You do not have permission to access this resource.");
    }

    // --- SPRING SYSTEM EXCEPTIONS ---

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        logger.error("Data integrity violation: ", ex);

        return buildResponse(HttpStatus.CONFLICT,
                "Bad Request", "Data integrity violation: " +
                        "possibly a duplicate unique value (e.g. teamName already exists) " +
                        "or invalid reference.");
    }

    // Handles validation errors (when using @Valid) and concatenates them into a single string
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation Failed", errors);
    }

    // Handles malformed JSON requests (e.g., missing commas, incorrect data types)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed JSON Request", "The JSON request body is invalid or cannot be parsed.");
    }

    // Handles URL parameter type mismatches (e.g., /api/staff/abc instead of /api/staff/1)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Parameter '%s' should be of type '%s'", ex.getName(), ex.getRequiredType().getSimpleName());
        return buildResponse(HttpStatus.BAD_REQUEST, "Type Mismatch", message);
    }

    // --- GLOBAL FALLBACK (For any unhandled exceptions) ---

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        // Log the error stack trace for debugging purposes
        logger.error("Unhandled exception occurred", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred. Please try again later.");
    }

    // --- HELPER METHOD ---

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String error, String message) {
        ErrorResponse response = new ErrorResponse(
                status.value(),
                error,
                message,
                LocalDateTime.now()
        );
        return ResponseEntity.status(status).body(response);
    }
}
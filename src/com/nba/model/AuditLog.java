package com.nba.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String methodName;

    @Column(columnDefinition = "TEXT")
    private String arguments;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public AuditLog() {}

    public AuditLog(String methodName, String arguments) {
        this.methodName = methodName;
        this.arguments = arguments;
        this.timestamp = LocalDateTime.now();
    }

    public Integer getId() { return id; }
    public String getMethodName() { return methodName; }
    public String getArguments() { return arguments; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
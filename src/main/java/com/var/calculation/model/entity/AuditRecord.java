package com.var.calculation.model.entity;

import com.var.calculation.model.enums.AuditStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Audit record entity for tracking API requests and responses.
 * Stores execution metrics and error information for monitoring and compliance.
 * 
 * Updated for Jakarta Persistence (JPA 3.0).
 */
@Entity
@Table(name = "audit_record")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private String endpoint;
    
    @Column(columnDefinition = "TEXT")
    private String requestPayload;
    
    @Column(columnDefinition = "TEXT")
    private String responsePayload;
    
    @Column(nullable = false)
    private Long executionTimeMs;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditStatus status;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
}

package com.var.calculation.service;

import com.var.calculation.model.entity.AuditRecord;
import com.var.calculation.model.enums.AuditStatus;
import com.var.calculation.repository.AuditRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {
    
    private final AuditRecordRepository auditRepository;
    
    @Async
    public void logRequest(String userId, String endpoint, long executionTime, 
                          boolean success, String errorMessage) {
        AuditRecord record = AuditRecord.builder()
                .userId(userId)
                .endpoint(endpoint)
                .executionTimeMs(executionTime)
                .status(success ? AuditStatus.SUCCESS : AuditStatus.ERROR)
                .errorMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
        
        auditRepository.save(record);
    }
    
    public List<AuditRecord> getAuditHistory() {
        return auditRepository.findAll();
    }
}

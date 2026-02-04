package com.var.calculation.repository;

import com.var.calculation.model.entity.AuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRecordRepository extends JpaRepository<AuditRecord, Long> {
}

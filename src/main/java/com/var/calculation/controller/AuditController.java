package com.var.calculation.controller;

import com.var.calculation.model.entity.AuditRecord;
import com.var.calculation.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Audit history endpoints (ADMIN only)")
@SecurityRequirement(name = "Bearer Authentication")
public class AuditController {
    
    private final AuditService auditService;
    
    @GetMapping("/history")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get audit history (ADMIN only)")
    public ResponseEntity<List<AuditRecord>> getAuditHistory() {
        return ResponseEntity.ok(auditService.getAuditHistory());
    }
}

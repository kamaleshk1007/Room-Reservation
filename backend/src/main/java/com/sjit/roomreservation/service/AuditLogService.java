package com.sjit.roomreservation.service;

import com.sjit.roomreservation.entity.AuditLog;
import com.sjit.roomreservation.entity.User;
import com.sjit.roomreservation.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String entityType, String entityId, String action, User performedBy, String details) {
        AuditLog auditLog = new AuditLog(entityType, entityId, action, performedBy);
        // Wrap details in JSON format for the JSON column
        if (details != null) {
            auditLog.setDetails("\"" + details.replace("\"", "\\\"") + "\"");
        }
        auditLogRepository.save(auditLog);
    }
}
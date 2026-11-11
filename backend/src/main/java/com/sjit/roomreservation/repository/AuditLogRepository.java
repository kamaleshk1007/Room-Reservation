package com.sjit.roomreservation.repository;

import com.sjit.roomreservation.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repository for AuditLog entities.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
}
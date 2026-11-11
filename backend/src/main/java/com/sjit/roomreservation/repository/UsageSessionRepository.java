package com.sjit.roomreservation.repository;

import com.sjit.roomreservation.entity.UsageSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repository for UsageSession entities.
 */
public interface UsageSessionRepository extends JpaRepository<UsageSession, String> {
}
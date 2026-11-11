package com.sjit.roomreservation.repository;

import com.sjit.roomreservation.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

/**
 * Repository for User entities.
 * Provides lookup by portal_staff_id (maps to college portal SSO).
 */
public interface UserRepository extends JpaRepository<User, String> {

    /** Find user by portal staff ID (for cookie-based SSO mapping). */
    Optional<User> findByPortalStaffId(String portalStaffId);

    /** Find active users. */
    // Optional<User> findByIsActiveTrue();

    /** Find users by role. */
    // Optional<List<User>> findByRole(String role);
}
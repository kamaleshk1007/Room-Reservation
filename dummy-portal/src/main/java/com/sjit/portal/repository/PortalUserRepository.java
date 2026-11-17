package com.sjit.portal.repository;

import com.sjit.portal.entity.PortalUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PortalUserRepository extends JpaRepository<PortalUser, String> {

    Optional<PortalUser> findByStaffId(String staffId);

    boolean existsByStaffId(String staffId);
}

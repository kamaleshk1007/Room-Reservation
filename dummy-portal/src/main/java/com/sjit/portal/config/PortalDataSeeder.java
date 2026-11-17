package com.sjit.portal.config;

import com.sjit.portal.entity.PortalUser;
import com.sjit.portal.repository.PortalUserRepository;
import com.sjit.portal.service.PortalUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Seeds the dummy portal users to mirror Room Reserve's seeded accounts.
 * staff002 exists only to prove the 403 case (valid portal staff, unknown to Room Reserve).
 * Passwords come from environment variables, never from source.
 */
@Component
public class PortalDataSeeder implements ApplicationRunner {

    private final PortalUserRepository repository;
    private final PortalUserService portalUserService;

    @Value("${portal.seed.hod.password}")
    private String hodPassword;

    @Value("${portal.seed.staff.password}")
    private String staffPassword;

    @Value("${portal.seed.staff002.password}")
    private String staff002Password;

    public PortalDataSeeder(PortalUserRepository repository, PortalUserService portalUserService) {
        this.repository = repository;
        this.portalUserService = portalUserService;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed("hod001", "Head of Department", hodPassword, "HOD");
        seed("staff001", "Staff User", staffPassword, "STAFF");
        seed("staff002", "Portal Only User", staff002Password, "STAFF");
    }

    private void seed(String staffId, String name, String password, String role) {
        if (repository.existsByStaffId(staffId)) {
            return;
        }
        PortalUser user = new PortalUser();
        user.setStaffId(staffId);
        user.setName(name);
        user.setRole(role);
        user.setPasswordHash(portalUserService.hash(password));
        user.setCreatedAt(LocalDateTime.now());
        repository.save(user);
    }
}

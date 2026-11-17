package com.sjit.portal.service;

import com.sjit.portal.entity.PortalUser;
import com.sjit.portal.repository.PortalUserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PortalUserService {

    private final PortalUserRepository repository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public PortalUserService(PortalUserRepository repository) {
        this.repository = repository;
    }

    public Optional<PortalUser> findByStaffId(String staffId) {
        return repository.findByStaffId(staffId);
    }

    public boolean matches(PortalUser user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }

    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}

package com.sjit.roomreservation.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import com.sjit.roomreservation.entity.User;
import com.sjit.roomreservation.repository.UserRepository;

@Component
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String portalStaffId) {
        User user = userRepository.findByPortalStaffId(portalStaffId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with portal_staff_id: " + portalStaffId));

        // Return Spring Security User with authorities from role
        // Role values from DB: STAFF, HOD, LAB_INCHARGE
        // Map to Spring Security ROLE_ prefix
        String role = "ROLE_" + user.getRole();
        
        org.springframework.security.core.userdetails.User.UserBuilder builder = 
            org.springframework.security.core.userdetails.User.withUsername(user.getPortalStaffId());
        builder.password(user.getPasswordHash());
        builder.authorities(role);
        
        return builder.build();
    }
}
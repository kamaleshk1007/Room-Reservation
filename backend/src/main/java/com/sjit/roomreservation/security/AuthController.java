package com.sjit.roomreservation.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.context.SecurityContextHolder;
import com.sjit.roomreservation.dto.UserSummary;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.sjit.roomreservation.entity.RefreshToken;
import com.sjit.roomreservation.entity.User;
import com.sjit.roomreservation.repository.RefreshTokenRepository;
import com.sjit.roomreservation.repository.UserRepository;
import java.time.LocalDateTime;

/**
 * Authentication controller for interim JWT-based login.
 * Login uses portal_staff_id + bcrypt password verification.
 * Refresh and logout use refresh tokens stored in DB.
 * Portal-cookie SSO integration pending (cookie name/domain unknown).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getPortalStaffId(),
                            loginRequest.getPassword()
                    )
            );

            String username = loginRequest.getPortalStaffId();
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            String accessToken = jwtUtil.generateAccessToken(username);
            String refreshToken = jwtUtil.generateRefreshToken(username);

            User user = userRepository.findByPortalStaffId(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            RefreshToken rt = new RefreshToken();
            rt.setUser(user);
            rt.setToken(refreshToken);
            // Use LocalDateTime instead of Date for RefreshToken entity
            rt.setExpiresAt(LocalDateTime.now().plusMinutes(7 * 24 * 60));
            rt.setIsRevoked(false);
            rt.setCreatedAt(LocalDateTime.now());
            refreshTokenRepository.save(rt);

            jwtUtil.setRefreshTokenCookie(response, refreshToken);

            return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthError("Invalid portal_staff_id or password"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = jwtUtil.getRawToken(request);
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthError("Refresh token required"));
        }

        try {
            String username = jwtUtil.extractUsername(refreshToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            RefreshToken storedToken = refreshTokenRepository
                    .findByTokenAndValid(refreshToken)
                    .orElseThrow(() -> new SecurityException("Invalid refresh token"));

            if (jwtUtil.isTokenExpired(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new AuthError("Refresh token expired"));
            }

            String newAccessToken = jwtUtil.generateAccessToken(username);

            storedToken.setCreatedAt(LocalDateTime.now());
            refreshTokenRepository.save(storedToken);

            jwtUtil.setRefreshTokenCookie(response, refreshToken);

            return ResponseEntity.ok(new AuthResponse(newAccessToken, refreshToken));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthError("Invalid refresh token: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = jwtUtil.getRawToken(request);
        if (refreshToken != null) {
            refreshTokenRepository.findByTokenAndValid(refreshToken)
                    .ifPresent(rt -> {
                        rt.setIsRevoked(true);
                        refreshTokenRepository.save(rt);
                    });
        }
        jwtUtil.clearRefreshTokenCookie(response);
        return ResponseEntity.ok(new AuthSuccess("Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthError("Not authenticated"));
        }
        String portalStaffId = authentication.getName();
        User user = userRepository.findByPortalStaffId(portalStaffId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return ResponseEntity.ok(new UserSummary(
                user.getId(),
                user.getPortalStaffId(),
                user.getName(),
                user.getRole()
        ));
    }

    public static class LoginRequest {
        private String portalStaffId;
        private String password;

        public String getPortalStaffId() { return portalStaffId; }
        public void setPortalStaffId(String portalStaffId) { this.portalStaffId = portalStaffId; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;

        public AuthResponse() {}

        public AuthResponse(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }

    public static class AuthError {
        private String message;

        public AuthError() {}

        public AuthError(String message) {
            this.message = message;
        }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class AuthSuccess {
        private String message;

        public AuthSuccess() {}

        public AuthSuccess(String message) {
            this.message = message;
        }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
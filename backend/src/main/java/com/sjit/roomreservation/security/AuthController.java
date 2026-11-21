package com.sjit.roomreservation.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import com.sjit.roomreservation.exception.ReplayedHandoffTokenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.sjit.roomreservation.entity.RefreshToken;
import com.sjit.roomreservation.entity.User;
import com.sjit.roomreservation.repository.RefreshTokenRepository;
import com.sjit.roomreservation.repository.UserRepository;
import javax.crypto.SecretKey;
import java.time.LocalDateTime;

/**
 * Authentication controller for interim JWT-based login and portal-cookie SSO.
 * Login uses portal_staff_id + bcrypt password verification.
 * Portal SSO trusts the PORTAL_SESSION cookie issued by the college portal.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String PORTAL_COOKIE_NAME = "PORTAL_SESSION";

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

    @Value("${app.portal.cookieSecret:}")
    private String portalCookieSecret;

    @Autowired
    private PortalHandoffVerifier portalHandoffVerifier;

    private SecretKey portalKey;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getPortalStaffId(),
                            loginRequest.getPassword()
                    )
            );

            String username = loginRequest.getPortalStaffId();
            User user = userRepository.findByPortalStaffId(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            return issueTokens(user, username, response);
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

    @PostMapping("/portal-sso")
    public ResponseEntity<?> portalSso(@CookieValue(value = PORTAL_COOKIE_NAME, required = false) String session,
                                       HttpServletResponse response) {
        if (portalCookieSecret == null || portalCookieSecret.isBlank()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new AuthError("Portal SSO is not configured on this server."));
        }
        if (session == null || session.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthError("Portal session is missing. Sign in at the college portal first."));
        }

        String staffId;
        try {
            staffId = Jwts.parser()
                    .verifyWith(portalCookieKey())
                    .build()
                    .parseSignedClaims(session)
                    .getPayload()
                    .getSubject();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthError("Invalid or expired portal session"));
        }
        if (staffId == null || staffId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthError("Invalid or expired portal session"));
        }

        User user = userRepository.findByPortalStaffId(staffId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new AuthError("Staff ID '" + staffId + "' is not registered in Room Reserve. "
                            + "Ask the Room Reserve administrator to grant you access."));
        }

        return issueTokens(user, staffId, response);
    }

    /**
     * Accepts the short-lived token handed over by the portal in the URL fragment.
     * The token is single use and audience-scoped to Room Reserve.
     */
    @PostMapping("/portal-handoff")
    public ResponseEntity<?> portalHandoff(@Valid @RequestBody PortalHandoffRequest request,
                                           HttpServletResponse response) {
        if (!portalHandoffVerifier.isConfigured()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new AuthError("Portal hand-off is not configured on this server."));
        }

        String staffId;
        try {
            staffId = portalHandoffVerifier.verifyAndConsume(request.getToken());
        } catch (ReplayedHandoffTokenException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthError("Hand-off token has already been used."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthError("Invalid or expired hand-off token."));
        }

        User user = userRepository.findByPortalStaffId(staffId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new AuthError("Staff ID '" + staffId + "' is not registered in Room Reserve. "
                            + "Ask the Room Reserve administrator to grant you access."));
        }

        return issueTokens(user, staffId, response);
    }

    private ResponseEntity<AuthResponse> issueTokens(User user, String staffId, HttpServletResponse response) {
        String accessToken = jwtUtil.generateAccessToken(staffId);
        String refreshToken = jwtUtil.generateRefreshToken(staffId);

        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setToken(refreshToken);
        rt.setExpiresAt(LocalDateTime.now().plusMinutes(7L * 24 * 60));
        rt.setIsRevoked(false);
        rt.setCreatedAt(LocalDateTime.now());
        refreshTokenRepository.save(rt);

        jwtUtil.setRefreshTokenCookie(response, refreshToken);

        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken));
    }

    private SecretKey portalCookieKey() {
        if (portalKey == null) {
            byte[] secretBytes = Decoders.BASE64.decode(portalCookieSecret);
            if (secretBytes.length < 32) {
                throw new IllegalStateException("app.portal.cookieSecret must be base64-encoded and decode to at least 32 bytes");
            }
            portalKey = Keys.hmacShaKeyFor(secretBytes);
        }
        return portalKey;
    }

    public static class PortalHandoffRequest {
        @NotBlank(message = "token is required")
        private String token;

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
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

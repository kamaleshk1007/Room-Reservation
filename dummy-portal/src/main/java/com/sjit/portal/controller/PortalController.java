package com.sjit.portal.controller;

import com.sjit.portal.entity.PortalUser;
import com.sjit.portal.service.PortalTokenService;
import com.sjit.portal.service.PortalUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/portal")
public class PortalController {

    public static final String COOKIE_NAME = PortalTokenService.COOKIE_NAME;

    private final PortalUserService portalUserService;
    private final PortalTokenService portalTokenService;
    private final boolean cookieSecure;

    public PortalController(PortalUserService portalUserService,
                            PortalTokenService portalTokenService,
                            @org.springframework.beans.factory.annotation.Value("${portal.cookie-secure:false}") boolean cookieSecure) {
        this.portalUserService = portalUserService;
        this.portalTokenService = portalTokenService;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody PortalLoginRequest request, HttpServletResponse response) {
        Optional<PortalUser> user = portalUserService.findByStaffId(request.getStaffId());
        if (user.isEmpty() || !portalUserService.matches(user.get(), request.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid staff ID or password"));
        }
        String staffId = user.get().getStaffId();
        String token = portalTokenService.createToken(staffId);
        response.addHeader("Set-Cookie", buildCookie(token, portalTokenService.getTtlSeconds()));
        PortalUser u = user.get();
        return ResponseEntity.ok(Map.of("staffId", u.getStaffId(), "name", u.getName(), "role", u.getRole()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        response.addHeader("Set-Cookie", buildCookie("", 0));
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@CookieValue(value = COOKIE_NAME, required = false) String session) {
        if (session == null || session.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Portal session is missing"));
        }
        String staffId = portalTokenService.verifyToken(session);
        if (staffId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired portal session"));
        }
        Optional<PortalUser> user = portalUserService.findByStaffId(staffId);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Portal session is not recognized"));
        }
        PortalUser u = user.get();
        return ResponseEntity.ok(Map.of("staffId", u.getStaffId(), "name", u.getName(), "role", u.getRole()));
    }

    private String buildCookie(String value, long maxAgeSeconds) {
        StringBuilder cookie = new StringBuilder(COOKIE_NAME)
                .append('=')
                .append(value)
                .append("; Path=/")
                .append("; HttpOnly")
                .append("; SameSite=Lax")
                .append("; Max-Age=").append(maxAgeSeconds);
        if (cookieSecure) {
            cookie.append("; Secure");
        }
        return cookie.toString();
    }

    public static class PortalLoginRequest {
        @NotBlank(message = "staffId is required")
        private String staffId;

        @NotBlank(message = "password is required")
        private String password;

        public String getStaffId() { return staffId; }
        public void setStaffId(String staffId) { this.staffId = staffId; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}

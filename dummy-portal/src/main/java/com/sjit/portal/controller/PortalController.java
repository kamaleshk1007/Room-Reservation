package com.sjit.portal.controller;

import com.sjit.portal.entity.PortalUser;
import com.sjit.portal.service.PortalHandoffService;
import com.sjit.portal.service.PortalTokenService;
import com.sjit.portal.service.PortalUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/portal")
public class PortalController {

    public static final String COOKIE_NAME = PortalTokenService.COOKIE_NAME;

    private static final String DEFAULT_ROOM_RESERVE_URL = "http://localhost:5173";

    /** Plain domain labels, optionally prefixed with a dot for the shared parent domain. */
    private static final Pattern COOKIE_DOMAIN_PATTERN = Pattern.compile(
            "^\\.?[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?(\\.[A-Za-z0-9]([A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*$");

    private final PortalUserService portalUserService;
    private final PortalTokenService portalTokenService;
    private final PortalHandoffService portalHandoffService;
    private final boolean cookieSecure;
    private final String cookieDomain;
    private final String roomReserveUrl;

    public PortalController(PortalUserService portalUserService,
                            PortalTokenService portalTokenService,
                            PortalHandoffService portalHandoffService,
                            @Value("${portal.cookie-secure:false}") boolean cookieSecure,
                            @Value("${portal.cookie-domain:}") String cookieDomain,
                            @Value("${portal.room-reserve-url:" + DEFAULT_ROOM_RESERVE_URL + "}") String roomReserveUrl) {
        this.portalUserService = portalUserService;
        this.portalTokenService = portalTokenService;
        this.portalHandoffService = portalHandoffService;
        this.cookieSecure = cookieSecure;
        this.cookieDomain = validateCookieDomain(cookieDomain);
        this.roomReserveUrl = validateRoomReserveUrl(roomReserveUrl);
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

    /** Public page configuration: where the portal should send staff when they open Room Reserve. */
    @GetMapping("/config")
    public ResponseEntity<?> config() {
        return ResponseEntity.ok(Map.of("roomReserveUrl", roomReserveUrl));
    }

    /**
     * Issues the short-lived, single-use token for the token hand-off flow.
     * Requires a valid portal session cookie; the token is signed with PORTAL_HANDOFF_SECRET.
     */
    @PostMapping("/handoff")
    public ResponseEntity<?> handoff(@CookieValue(value = COOKIE_NAME, required = false) String session) {
        if (!portalHandoffService.isConfigured()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Token hand-off is not configured on this server."));
        }
        if (session == null || session.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Portal session is missing"));
        }
        String staffId = portalTokenService.verifyToken(session);
        if (staffId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired portal session"));
        }
        return ResponseEntity.ok(Map.of("token", portalHandoffService.createToken(staffId)));
    }

    private String buildCookie(String value, long maxAgeSeconds) {
        StringBuilder cookie = new StringBuilder(COOKIE_NAME)
                .append('=')
                .append(value)
                .append("; Path=/")
                .append("; HttpOnly")
                .append("; SameSite=Lax")
                .append("; Max-Age=").append(maxAgeSeconds);
        if (!cookieDomain.isEmpty()) {
            cookie.append("; Domain=").append(cookieDomain);
        }
        if (cookieSecure) {
            cookie.append("; Secure");
        }
        return cookie.toString();
    }

    private static String validateCookieDomain(String value) {
        String domain = value == null ? "" : value.trim();
        if (domain.isEmpty()) {
            return "";
        }
        if (!COOKIE_DOMAIN_PATTERN.matcher(domain).matches()) {
            throw new IllegalArgumentException(
                    "portal.cookie-domain must be a plain domain such as .college.edu, or empty on localhost");
        }
        return domain;
    }

    private static String validateRoomReserveUrl(String value) {
        String url = value == null ? "" : value.trim();
        if (url.isEmpty()) {
            url = DEFAULT_ROOM_RESERVE_URL;
        }
        try {
            URI uri = new URI(url);
            boolean httpScheme = "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
            if (!httpScheme || uri.getHost() == null || uri.getQuery() != null || uri.getFragment() != null) {
                throw new IllegalArgumentException(
                        "portal.room-reserve-url must be an absolute http(s) URL without query or fragment");
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("portal.room-reserve-url must be a valid URL", e);
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
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

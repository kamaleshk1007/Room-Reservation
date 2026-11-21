package com.sjit.roomreservation.security;

import com.sjit.roomreservation.entity.User;
import com.sjit.roomreservation.repository.RefreshTokenRepository;
import com.sjit.roomreservation.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the portal-cookie SSO endpoint with real HS256 tokens and mocked persistence.
 * No database or Spring context is needed: AuthController is built standalone.
 */
class PortalSsoControllerTest {

    private static final String PORTAL_SECRET = Base64.getEncoder()
            .encodeToString("portal-test-secret-0123456789abc".getBytes(StandardCharsets.UTF_8));
    private static final String OTHER_SECRET = Base64.getEncoder()
            .encodeToString("other-test-secret-0123456789abcd".getBytes(StandardCharsets.UTF_8));

    private JwtUtil jwtUtil;
    private UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private AuthController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        userRepository = mock(UserRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);

        controller = controllerWithSecret(PORTAL_SECRET);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void acceptsSignedPortalSessionAndIssuesTokens() throws Exception {
        when(userRepository.findByPortalStaffId("staff001")).thenReturn(Optional.of(user("staff001")));
        when(jwtUtil.generateAccessToken("staff001")).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken("staff001")).thenReturn("refresh-token");

        mockMvc.perform(post("/api/auth/portal-sso")
                        .cookie(sessionCookie(token("staff001", Instant.now().plusSeconds(600), PORTAL_SECRET))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void rejectsExpiredPortalSession() throws Exception {
        mockMvc.perform(post("/api/auth/portal-sso")
                        .cookie(sessionCookie(token("staff001", Instant.now().minusSeconds(60), PORTAL_SECRET))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTamperedPortalSession() throws Exception {
        mockMvc.perform(post("/api/auth/portal-sso")
                        .cookie(sessionCookie(token("staff001", Instant.now().plusSeconds(600), OTHER_SECRET))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsMissingPortalSession() throws Exception {
        mockMvc.perform(post("/api/auth/portal-sso"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsStaffUnknownToRoomReserve() throws Exception {
        when(userRepository.findByPortalStaffId("staff002")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/portal-sso")
                        .cookie(sessionCookie(token("staff002", Instant.now().plusSeconds(600), PORTAL_SECRET))))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsServiceUnavailableWhenSecretIsNotConfigured() throws Exception {
        AuthController unconfigured = controllerWithSecret("");
        MockMvc unconfiguredMvc = MockMvcBuilders.standaloneSetup(unconfigured).build();

        unconfiguredMvc.perform(post("/api/auth/portal-sso")
                        .cookie(sessionCookie(token("staff001", Instant.now().plusSeconds(600), PORTAL_SECRET))))
                .andExpect(status().isServiceUnavailable());
    }

    private AuthController controllerWithSecret(String secret) {
        AuthController authController = new AuthController();
        ReflectionTestUtils.setField(authController, "authenticationManager", mock(AuthenticationManager.class));
        ReflectionTestUtils.setField(authController, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(authController, "userRepository", userRepository);
        ReflectionTestUtils.setField(authController, "passwordEncoder", mock(PasswordEncoder.class));
        ReflectionTestUtils.setField(authController, "refreshTokenRepository", refreshTokenRepository);
        ReflectionTestUtils.setField(authController, "userDetailsService", mock(UserDetailsService.class));
        ReflectionTestUtils.setField(authController, "portalCookieSecret", secret);
        return authController;
    }

    private static User user(String staffId) {
        User user = new User();
        user.setPortalStaffId(staffId);
        user.setName("Staff User");
        user.setRole("STAFF");
        return user;
    }

    private static Cookie sessionCookie(String token) {
        return new Cookie("PORTAL_SESSION", token);
    }

    private static String token(String subject, Instant expiration, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        Date issuedAt = Date.from(expiration.minusSeconds(300));
        return Jwts.builder()
                .subject(subject)
                .issuedAt(issuedAt)
                .expiration(Date.from(expiration))
                .signWith(key)
                .compact();
    }
}

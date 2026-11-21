package com.sjit.roomreservation.security;

import com.sjit.roomreservation.entity.User;
import com.sjit.roomreservation.repository.RefreshTokenRepository;
import com.sjit.roomreservation.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the token hand-off endpoint with real HS256 tokens and mocked persistence.
 */
class PortalHandoffControllerTest {

    private static final String HANDOFF_SECRET = Base64.getEncoder()
            .encodeToString("handoff-test-secret-0123456789ab".getBytes(StandardCharsets.UTF_8));
    private static final String OTHER_SECRET = Base64.getEncoder()
            .encodeToString("tampered-test-secret-0123456789a".getBytes(StandardCharsets.UTF_8));

    private JwtUtil jwtUtil;
    private UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private PortalHandoffVerifier verifier;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        userRepository = mock(UserRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        verifier = new PortalHandoffVerifier(HANDOFF_SECRET);

        AuthController controller = controllerWithVerifier(verifier);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void acceptsValidHandoffTokenAndIssuesTokens() throws Exception {
        when(userRepository.findByPortalStaffId("staff001")).thenReturn(Optional.of(user("staff001")));
        when(jwtUtil.generateAccessToken("staff001")).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken("staff001")).thenReturn("refresh-token");

        postToken(token("staff001", "room-reserve", Instant.now().plusSeconds(60), UUID.randomUUID().toString(), HANDOFF_SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }

    @Test
    void rejectsExpiredHandoffToken() throws Exception {
        postToken(token("staff001", "room-reserve", Instant.now().minusSeconds(5), UUID.randomUUID().toString(), HANDOFF_SECRET))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired hand-off token."));
    }

    @Test
    void rejectsWrongAudience() throws Exception {
        postToken(token("staff001", "another-app", Instant.now().plusSeconds(60), UUID.randomUUID().toString(), HANDOFF_SECRET))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired hand-off token."));
    }

    @Test
    void rejectsTamperedToken() throws Exception {
        postToken(token("staff001", "room-reserve", Instant.now().plusSeconds(60), UUID.randomUUID().toString(), OTHER_SECRET))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid or expired hand-off token."));
    }

    @Test
    void rejectsReplayedTokenId() throws Exception {
        when(userRepository.findByPortalStaffId("staff001")).thenReturn(Optional.of(user("staff001")));
        when(jwtUtil.generateAccessToken("staff001")).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken("staff001")).thenReturn("refresh-token");

        String handoff = token("staff001", "room-reserve", Instant.now().plusSeconds(60), "reused-id", HANDOFF_SECRET);

        postToken(handoff).andExpect(status().isOk());
        postToken(handoff)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Hand-off token has already been used."));
    }

    @Test
    void rejectsStaffUnknownToRoomReserve() throws Exception {
        when(userRepository.findByPortalStaffId("staff002")).thenReturn(Optional.empty());

        postToken(token("staff002", "room-reserve", Instant.now().plusSeconds(60), UUID.randomUUID().toString(), HANDOFF_SECRET))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsServiceUnavailableWhenSecretIsNotConfigured() throws Exception {
        AuthController unconfigured = controllerWithVerifier(new PortalHandoffVerifier(""));
        MockMvc unconfiguredMvc = MockMvcBuilders.standaloneSetup(unconfigured).build();

        unconfiguredMvc.perform(post("/api/auth/portal-handoff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(token("staff001", "room-reserve", Instant.now().plusSeconds(60),
                                UUID.randomUUID().toString(), HANDOFF_SECRET))))
                .andExpect(status().isServiceUnavailable());
    }

    private ResultActions postToken(String token) throws Exception {
        return mockMvc.perform(post("/api/auth/portal-handoff")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody(token)));
    }

    private static String requestBody(String token) {
        return "{\"token\":\"" + token + "\"}";
    }

    private AuthController controllerWithVerifier(PortalHandoffVerifier handoffVerifier) {
        AuthController controller = new AuthController();
        ReflectionTestUtils.setField(controller, "authenticationManager", mock(AuthenticationManager.class));
        ReflectionTestUtils.setField(controller, "jwtUtil", jwtUtil);
        ReflectionTestUtils.setField(controller, "userRepository", userRepository);
        ReflectionTestUtils.setField(controller, "passwordEncoder", mock(PasswordEncoder.class));
        ReflectionTestUtils.setField(controller, "refreshTokenRepository", refreshTokenRepository);
        ReflectionTestUtils.setField(controller, "userDetailsService", mock(UserDetailsService.class));
        ReflectionTestUtils.setField(controller, "portalCookieSecret", "");
        ReflectionTestUtils.setField(controller, "portalHandoffVerifier", handoffVerifier);
        return controller;
    }

    private static User user(String staffId) {
        User user = new User();
        user.setPortalStaffId(staffId);
        user.setName("Staff User");
        user.setRole("STAFF");
        return user;
    }

    private static String token(String subject, String audience, Instant expiration, String tokenId, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        Date issuedAt = Date.from(expiration.minusSeconds(30));
        return Jwts.builder()
                .id(tokenId)
                .subject(subject)
                .audience().add(audience).and()
                .issuedAt(issuedAt)
                .expiration(Date.from(expiration))
                .signWith(key)
                .compact();
    }
}

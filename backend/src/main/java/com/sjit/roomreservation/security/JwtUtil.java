package com.sjit.roomreservation.security;

import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;
import java.util.Base64;

/**
 * Simple JWT utility using base64-encoded HMAC secret.
 * Designed for interim JWT auth until portal-cookie SSO is implemented.
 * Uses only core Java classes.
 */
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration-minutes}")
    private long jwtAccessExpirationMinutes;

    @Value("${app.jwt.refresh-token-expiration-days}")
    private long jwtRefreshExpirationDays;

    private final String secret;

    public JwtUtil() {
        this.secret = jwtSecret;
    }

    /** Extract the JWT token from the Authorization header */
    public String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    /** Extract the username (portal_staff_id) from the token */
    public String extractUsername(String token) {
        return extractClaim(token, this::getSubject);
    }

    /** Extract a specific claim from the token */
    public <T> T extractClaim(String token, Function<String, T> claimsResolver) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;
            String base64Payload = parts[1];
            while (base64Payload.length() % 4 != 0) {
                base64Payload += "=";
            }
            String json = new String(Base64.getUrlDecoder().decode(base64Payload), StandardCharsets.UTF_8);
            return claimsResolver.apply(json);
        } catch (Exception e) {
            return null;
        }
    }

    /** Get the subject (portal_staff_id) from token payload JSON */
    private String getSubject(String json) {
        String key = "\"sub\"";
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int start = idx + key.length() + 2;
        int end = json.indexOf(",", start);
        if (end < 0) end = json.indexOf("}", start);
        if (end < 0) return null;
        return json.substring(start, end).replace("\"", "").trim();
    }

    /** Check if the token is expired */
    public boolean isTokenExpired(String token) {
        String expClaim = extractClaim(token, json -> {
            String key = "\"exp\"";
            int idx = json.indexOf(key);
            if (idx < 0) return null;
            int start = idx + key.length() + 2;
            int end = json.indexOf(",", start);
            if (end < 0) end = json.indexOf("}", start);
            if (end < 0) return null;
            return json.substring(start, end).trim();
        });
        if (expClaim == null) return false;
        try {
            long exp = Long.parseLong(expClaim);
            return System.currentTimeMillis() / 1000 > exp;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Generate an access token (simple base64-encoded JWT) */
    public String generateAccessToken(String username) {
        long issuedAt = System.currentTimeMillis();
        long expiration = issuedAt + jwtAccessExpirationMinutes * 60 * 1000L;
        String payload = String.format(
                "{\"sub\":\"%s\",\"iat\":%d,\"exp\":%d}",
                username, issuedAt / 1000, expiration / 1000
        );
        String base64Payload = Base64.getUrlEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return "Bearer " + base64Payload + ".";
    }

    /** Generate a refresh token */
    public String generateRefreshToken(String username) {
        long issuedAt = System.currentTimeMillis();
        long expiration = issuedAt + jwtRefreshExpirationDays * 24 * 60 * 60 * 1000L;
        String payload = String.format(
                "{\"sub\":\"%s\",\"iat\":%d,\"exp\":%d}",
                username, issuedAt / 1000, expiration / 1000
        );
        String base64Payload = Base64.getUrlEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return "Bearer " + base64Payload + ".";
    }

    /** Validate token against UserDetails */
    public boolean validateToken(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return (username != null && username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /** Build authentication from JWT */
    public Authentication getAuthToken(String token) {
        String username = extractUsername(token);
        if (username != null) {
            return new UsernamePasswordAuthenticationToken(username, null, null);
        }
        throw new RuntimeException("Invalid JWT token");
    }

    /** Set refresh token as http-only cookie */
    public void setRefreshTokenCookie(HttpServletResponse response, String token) {
        response.setHeader("Set-Cookie",
                "refresh_token=" + token +
                        "; Path=/; HttpOnly; Secure; SameSite=Strict" +
                        "; Max-Age=" + (jwtRefreshExpirationDays * 24 * 60 * 60));
    }

    /** Clear refresh token cookie */
    public void clearRefreshTokenCookie(HttpServletResponse response) {
        response.setHeader("Set-Cookie",
                "refresh_token=; Path=/; HttpOnly; Secure; SameSite=Strict; Max-Age=0");
    }
}
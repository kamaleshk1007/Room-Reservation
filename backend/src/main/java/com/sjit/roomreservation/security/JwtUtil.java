package com.sjit.roomreservation.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT utility using signed HS256 tokens via jjwt library (0.9.1).
 * Tokens do NOT include "Bearer " prefix - they are pure JWT strings.
 * Designed for interim auth until portal-cookie SSO is implemented.
 */
@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration-minutes}")
    private long jwtAccessExpirationMinutes;

    @Value("${app.jwt.refresh-token-expiration-days}")
    private long jwtRefreshExpirationDays;

    /** Get the raw JWT token (without "Bearer " prefix) from the Authorization header */
    public String getRawToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }

    /** Extract the username (portal_staff_id) from the JWT token */
    public String extractUsername(String token) {
        try {
            return Jwts
                    .parser()
                    .setSigningKey(jwtSecret.getBytes(StandardCharsets.UTF_8))
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    /** Check if the token is expired */
    public boolean isTokenExpired(String token) {
        try {
            return Jwts
                    .parser()
                    .setSigningKey(jwtSecret.getBytes(StandardCharsets.UTF_8))
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration()
                    .before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /** Generate an access token (signed HS256 via jjwt, without "Bearer " prefix) */
    public String generateAccessToken(String username) {
        return Jwts
                .builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtAccessExpirationMinutes * 60 * 1000))
                .signWith(SignatureAlgorithm.HS256, jwtSecret.getBytes(StandardCharsets.UTF_8))
                .compact();
    }

    /** Generate a refresh token (signed HS256 via jjwt, without "Bearer " prefix) */
    public String generateRefreshToken(String username) {
        return Jwts
                .builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtRefreshExpirationDays * 24 * 60 * 60 * 1000))
                .signWith(SignatureAlgorithm.HS256, jwtSecret.getBytes(StandardCharsets.UTF_8))
                .compact();
    }

    /** Build authentication from JWT token */
    public Authentication getAuthToken(String token) {
        String username = extractUsername(token);
        if (username != null) {
            return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(username, null, null);
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

    /** Validate token against UserDetails */
    public boolean validateToken(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return (username != null && username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
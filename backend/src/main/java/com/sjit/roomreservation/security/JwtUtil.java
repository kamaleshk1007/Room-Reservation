package com.sjit.roomreservation.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration-minutes}")
    private long jwtAccessExpirationMinutes;

    @Value("${app.jwt.refresh-token-expiration-days}")
    private long jwtRefreshExpirationDays;

    private final SecretKey key;

    public JwtUtil() {
        // jjwt 0.12.x: requires 32+ bytes (256 bits) base64-encoded secret
        byte[] secretBytes = Decoders.BASE64.decode(jwtSecret);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes (256 bits) when base64-decoded");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

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
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    /** Check if the token is expired */
    public boolean isTokenExpired(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration()
                    .before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /** Extract expiration date from the JWT token */
    private Date extractExpiration(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();
        } catch (Exception e) {
            return null;
        }
    }

    /** Validate token against UserDetails */
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username != null && username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /** Generate an access token (signed HS256 via jjwt 0.12.x, without "Bearer " prefix) */
    public String generateAccessToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtAccessExpirationMinutes * 60 * 1000))
                .signWith(SignatureAlgorithm.HS256, key)
                .compact();
    }

    /** Generate a refresh token (signed HS256 via jjwt 0.12.x, without "Bearer " prefix) */
    public String generateRefreshToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtRefreshExpirationDays * 24 * 60 * 60 * 1000))
                .signWith(SignatureAlgorithm.HS256, key)
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
}
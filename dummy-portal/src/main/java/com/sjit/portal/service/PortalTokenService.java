package com.sjit.portal.service;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Signs and verifies the PORTAL_SESSION cookie. The token is a plain HS256 JWT
 * whose subject is the staff_id. Room Reserve verifies the same token with the
 * same decoded secret.
 */
@Service
public class PortalTokenService {

    public static final String COOKIE_NAME = "PORTAL_SESSION";

    private final SecretKey key;
    private final long ttlSeconds;

    public PortalTokenService(@Value("${portal.cookie-secret}") String cookieSecret,
                              @Value("${portal.session-ttl-seconds:28800}") long ttlSeconds) {
        byte[] secretBytes = Decoders.BASE64.decode(cookieSecret);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("portal.cookie-secret must be base64-encoded and decode to at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.ttlSeconds = ttlSeconds;
    }

    public String createToken(String staffId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(staffId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlSeconds * 1000))
                .signWith(key)
                .compact();
    }

    /** Returns the staff_id when the token is valid, otherwise null. */
    public String verifyToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }
}

package com.sjit.portal.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Issues the short-lived, single-use token that the portal hands to Room Reserve.
 * The token is signed with PORTAL_HANDOFF_SECRET, independent of the session cookie
 * secret, and is scoped to Room Reserve through its audience.
 */
@Service
public class PortalHandoffService {

    public static final String AUDIENCE = "room-reserve";
    public static final long TTL_SECONDS = 60;

    private final SecretKey key;

    public PortalHandoffService(@Value("${portal.handoff-secret:}") String handoffSecret) {
        if (handoffSecret == null || handoffSecret.isBlank()) {
            this.key = null;
            return;
        }
        byte[] secretBytes = Decoders.BASE64.decode(handoffSecret);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException(
                    "portal.handoff-secret must be base64-encoded and decode to at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    /** Hand-off is optional locally: when no secret is configured the endpoint returns 503. */
    public boolean isConfigured() {
        return key != null;
    }

    public String createToken(String staffId) {
        if (key == null) {
            throw new IllegalStateException("portal.handoff-secret is not configured");
        }
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(staffId)
                .audience().add(AUDIENCE).and()
                .issuedAt(now)
                .expiration(new Date(now.getTime() + TTL_SECONDS * 1000))
                .signWith(key)
                .compact();
    }
}

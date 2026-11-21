package com.sjit.roomreservation.security;

import com.sjit.roomreservation.exception.ReplayedHandoffTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Verifies the short-lived token issued by the college portal for the token hand-off flow.
 * The token is signed with PORTAL_HANDOFF_SECRET, must name Room Reserve as its audience,
 * and can be consumed only once. Used ids are kept in memory until their expiry.
 */
@Component
public class PortalHandoffVerifier {

    public static final String AUDIENCE = "room-reserve";

    private final SecretKey key;
    private final ConcurrentMap<String, Instant> usedTokenIds = new ConcurrentHashMap<>();

    public PortalHandoffVerifier(@Value("${app.portal.handoffSecret:}") String handoffSecret) {
        if (handoffSecret == null || handoffSecret.isBlank()) {
            this.key = null;
            return;
        }
        byte[] secretBytes = Decoders.BASE64.decode(handoffSecret);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException(
                    "app.portal.handoffSecret must be base64-encoded and decode to at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
    }

    /** Hand-off is optional: when no secret is configured the endpoint returns 503. */
    public boolean isConfigured() {
        return key != null;
    }

    /**
     * Verifies signature, audience and expiry, records the token id as used, and returns the
     * portal staff id. Throws ReplayedHandoffTokenException when the id was seen before.
     * Expired ids are pruned on each call.
     */
    public String verifyAndConsume(String token) {
        if (key == null) {
            throw new IllegalStateException("app.portal.handoffSecret is not configured");
        }

        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (claims.getAudience() == null || !claims.getAudience().contains(AUDIENCE)) {
            throw new IllegalArgumentException("Hand-off token is not addressed to Room Reserve");
        }

        String staffId = claims.getSubject();
        String tokenId = claims.getId();
        Date expiration = claims.getExpiration();
        if (staffId == null || staffId.isBlank() || tokenId == null || tokenId.isBlank() || expiration == null) {
            throw new IllegalArgumentException("Hand-off token is missing required claims");
        }

        Instant now = Instant.now();
        usedTokenIds.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        if (usedTokenIds.putIfAbsent(tokenId, expiration.toInstant()) != null) {
            throw new ReplayedHandoffTokenException("Hand-off token has already been used");
        }

        return staffId;
    }
}

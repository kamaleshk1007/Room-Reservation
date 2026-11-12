package com.sjit.roomreservation.repository;

import com.sjit.roomreservation.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

/**
 * Repository for RefreshToken entities.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    /** Find active (non-revoked, non-expired) refresh token by token string. */
    @Query(
            value = """
                    SELECT rt FROM RefreshToken rt
                    WHERE rt.token = :token
                      AND rt.isRevoked = FALSE
                      AND rt.expiresAt > CURRENT_TIMESTAMP
                    """
    )
    Optional<RefreshToken> findByTokenAndValid(@Param("token") String token);

    /** Find all revoked tokens for a user. */
    // Optional<List<RefreshToken>> findByUserIdAndIsRevoked(String userId, boolean isRevoked);
}
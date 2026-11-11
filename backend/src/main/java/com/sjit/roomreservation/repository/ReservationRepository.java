package com.sjit.roomreservation.repository;

import com.sjit.roomreservation.entity.Reservation;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

/**
 * Repository for Reservation entities.
 * Includes a custom query to check for overlapping time ranges
 * (used by the overlap trigger, also exposed for service-layer checks).
 */
public interface ReservationRepository extends JpaRepository<Reservation, String> {

    /** Find reservations by room and status. */
    // Optional<List<Reservation>> findByRoomIdAndStatus(String roomId, String status);

    /** Find pending reservations for a room. */
    // Optional<List<Reservation>> findByRoomIdAndStatus(String roomId, String status);

    /** Check if a time range overlaps any confirmed/pending reservation for the given room. */
    @Query(
            value = """
                    SELECT COUNT(r) > 0
                    FROM Reservation r
                    WHERE r.room = :roomId
                      AND r.status IN ('CONFIRMED', 'PENDING')
                      AND r.id != :excludeId
                      AND r.startTime < :endTime
                      AND r.endTime > :startTime
                    """
    )
    boolean existsOverlapByRoom(@Param("roomId") String roomId,
                                @Param("excludeId") String excludeId,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);
}
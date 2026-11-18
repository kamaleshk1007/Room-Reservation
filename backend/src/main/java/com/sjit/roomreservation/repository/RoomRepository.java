package com.sjit.roomreservation.repository;

import com.sjit.roomreservation.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

/**
 * Repository for Room entities.
 */
public interface RoomRepository extends JpaRepository<Room, String> {

    /** Find room by code (e.g., "AV01", "AU01"). */
    Optional<Room> findByCode(String code);

    /** Check whether a room with the given code exists. */
    boolean existsByCode(String code);

    /**
     * Find room by ID holding a pessimistic write lock (SELECT ... FOR UPDATE).
     * Native query so Hibernate does not emit a MySQL-specific "FOR UPDATE OF <alias>"
     * clause that TiDB rejects. Runs in the caller's transaction.
     */
    @Query(value = "SELECT * FROM rooms WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Room> findByIdForUpdate(@Param("id") String id);
}

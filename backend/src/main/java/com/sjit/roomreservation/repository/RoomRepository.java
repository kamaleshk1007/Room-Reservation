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

    /** Find active rooms only. */
    // @Query("SELECT r FROM Room r WHERE r.status = 'ACTIVE'")
    // Optional<Room> findByStatusActive();
}
package com.sjit.roomreservation.repository;

import com.sjit.roomreservation.entity.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

/**
 * Repository for Room entities.
 */
public interface RoomRepository extends JpaRepository<Room, String> {

    /** Find room by code (e.g., "AV01", "AU01"). */
    Optional<Room> findByCode(String code);

    /** Find room by ID with pessimistic write lock (SELECT FOR UPDATE). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") String id);
}
package com.sjit.roomreservation.service;

import com.sjit.roomreservation.dto.RoomCreateRequest;
import com.sjit.roomreservation.dto.RoomUpdateRequest;
import com.sjit.roomreservation.entity.Room;
import com.sjit.roomreservation.entity.User;
import com.sjit.roomreservation.exception.ResourceConflictException;
import com.sjit.roomreservation.repository.ReservationRepository;
import com.sjit.roomreservation.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final AuditLogService auditLogService;

    public RoomService(RoomRepository roomRepository, ReservationRepository reservationRepository,
                       AuditLogService auditLogService) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.auditLogService = auditLogService;
    }

    /** Create a room. Code must be unique (409 on conflict). */
    @Transactional
    public Room createRoom(RoomCreateRequest request, User actor) {
        String code = trimToNull(request.getCode());
        if (roomRepository.existsByCode(code)) {
            throw new ResourceConflictException("A room with code '" + code + "' already exists.");
        }
        Room room = new Room(
                request.getName().trim(),
                code,
                request.getCapacity(),
                request.getRoomType().trim(),
                "ACTIVE",
                trimToNull(request.getLocation())
        );
        Room saved = roomRepository.save(room);

        auditLogService.log("Room", saved.getId(), "CREATE", actor,
                "Room created: code=" + saved.getCode() + ", name=" + saved.getName());

        return saved;
    }

    /** Update a room's name, capacity and location. */
    @Transactional
    public Room updateRoom(String id, RoomUpdateRequest request, User actor) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Room not found"));
        if (Boolean.FALSE.equals(room.getIsActive())) {
            throw new jakarta.persistence.EntityNotFoundException("Room not found");
        }

        room.setName(request.getName().trim());
        room.setCapacity(request.getCapacity());
        room.setLocation(trimToNull(request.getLocation()));
        room.setUpdatedAt(LocalDateTime.now());
        Room saved = roomRepository.save(room);

        auditLogService.log("Room", saved.getId(), "UPDATE", actor,
                "Room updated: name=" + saved.getName() + ", capacity=" + saved.getCapacity()
                        + ", location=" + saved.getLocation());

        return saved;
    }

    /**
     * Soft-delete a room (is_active=false). Blocked with 409 when the room has
     * any future CONFIRMED or PENDING reservation; the message states both counts.
     */
    @Transactional
    public Room deleteRoom(String id, User actor) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Room not found"));
        if (Boolean.FALSE.equals(room.getIsActive())) {
            throw new jakarta.persistence.EntityNotFoundException("Room not found");
        }

        LocalDateTime now = LocalDateTime.now();
        long confirmed = reservationRepository.countFutureByRoomAndStatus(id, "CONFIRMED", now);
        long pending = reservationRepository.countFutureByRoomAndStatus(id, "PENDING", now);
        long total = confirmed + pending;
        if (total > 0) {
            throw new ResourceConflictException(
                    "Cannot delete room '" + room.getName() + "': it has " + total + " upcoming reservation(s) ("
                            + confirmed + " confirmed, " + pending + " pending).");
        }

        room.setIsActive(false);
        room.setUpdatedAt(now);
        Room saved = roomRepository.save(room);

        auditLogService.log("Room", saved.getId(), "DELETE", actor,
                "Room soft-deleted: code=" + saved.getCode() + ", name=" + saved.getName());

        return saved;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

package com.sjit.roomreservation.controller;

import com.sjit.roomreservation.dto.RoomCreateRequest;
import com.sjit.roomreservation.dto.RoomSummary;
import com.sjit.roomreservation.dto.RoomUpdateRequest;
import com.sjit.roomreservation.entity.Room;
import com.sjit.roomreservation.entity.User;
import com.sjit.roomreservation.repository.RoomRepository;
import com.sjit.roomreservation.repository.UserRepository;
import com.sjit.roomreservation.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomService roomService;

    @GetMapping
    public ResponseEntity<List<RoomSummary>> getAllRooms() {
        List<RoomSummary> rooms = roomRepository.findAll().stream()
                .filter(r -> "ACTIVE".equals(r.getStatus()))
                .filter(r -> !Boolean.FALSE.equals(r.getIsActive()))
                .map(RoomController::toSummary)
                .collect(Collectors.toList());
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomSummary> getRoomById(@PathVariable String id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Room not found"));
        if (Boolean.FALSE.equals(room.getIsActive())) {
            throw new jakarta.persistence.EntityNotFoundException("Room not found");
        }
        if (!"ACTIVE".equals(room.getStatus())) {
            throw new jakarta.persistence.EntityNotFoundException("Room not found");
        }
        return ResponseEntity.ok(toSummary(room));
    }

    @PostMapping
    @PreAuthorize("hasRole('HOD')")
    public ResponseEntity<RoomSummary> createRoom(@Valid @RequestBody RoomCreateRequest request) {
        User hod = currentUser();
        Room saved = roomService.createRoom(request, hod);
        return ResponseEntity.status(HttpStatus.CREATED).body(toSummary(saved));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HOD')")
    public ResponseEntity<RoomSummary> updateRoom(@PathVariable String id, @Valid @RequestBody RoomUpdateRequest request) {
        User hod = currentUser();
        Room saved = roomService.updateRoom(id, request, hod);
        return ResponseEntity.ok(toSummary(saved));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HOD')")
    public ResponseEntity<Map<String, String>> deleteRoom(@PathVariable String id) {
        User hod = currentUser();
        Room deleted = roomService.deleteRoom(id, hod);
        return ResponseEntity.ok(Map.of("message",
                "Room '" + deleted.getCode() + "' deleted (soft). Future confirmed/pending reservations block this operation."));
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String portalStaffId = authentication.getName();
        return userRepository.findByPortalStaffId(portalStaffId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found"));
    }

    private static RoomSummary toSummary(Room r) {
        return new RoomSummary(
                r.getId(),
                r.getName(),
                r.getCode(),
                r.getCapacity(),
                r.getRoomType(),
                r.getStatus(),
                r.getLocation()
        );
    }
}

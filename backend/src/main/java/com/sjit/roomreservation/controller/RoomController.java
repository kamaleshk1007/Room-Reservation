package com.sjit.roomreservation.controller;

import com.sjit.roomreservation.dto.RoomSummary;
import com.sjit.roomreservation.entity.Room;
import com.sjit.roomreservation.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    @Autowired
    private RoomRepository roomRepository;

    @GetMapping
    public ResponseEntity<List<RoomSummary>> getAllRooms() {
        List<RoomSummary> rooms = roomRepository.findAll().stream()
                .filter(r -> "ACTIVE".equals(r.getStatus()))
                .map(r -> new RoomSummary(
                        r.getId(),
                        r.getName(),
                        r.getCode(),
                        r.getCapacity(),
                        r.getRoomType(),
                        r.getStatus()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomSummary> getRoomById(@PathVariable String id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Room not found"));
        if (!"ACTIVE".equals(room.getStatus())) {
            throw new jakarta.persistence.EntityNotFoundException("Room not found");
        }
        return ResponseEntity.ok(new RoomSummary(
                room.getId(),
                room.getName(),
                room.getCode(),
                room.getCapacity(),
                room.getRoomType(),
                room.getStatus()
        ));
    }
}
package com.sjit.roomreservation.controller;

import com.sjit.roomreservation.dto.ReservationRequest;
import com.sjit.roomreservation.dto.ReservationResponse;
import com.sjit.roomreservation.dto.RoomSummary;
import com.sjit.roomreservation.dto.UserSummary;
import com.sjit.roomreservation.entity.Reservation;
import com.sjit.roomreservation.entity.Room;
import com.sjit.roomreservation.entity.User;
import com.sjit.roomreservation.repository.RoomRepository;
import com.sjit.roomreservation.repository.UserRepository;
import com.sjit.roomreservation.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(@Valid @RequestBody ReservationRequest request) {
        // Validate start < end
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new IllegalArgumentException("start_time must be before end_time");
        }

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Room not found"));

        User requestedBy = userRepository.findById(request.getRequestedById())
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found"));

        Reservation reservation = new Reservation();
        reservation.setRoom(room);
        reservation.setRequestedBy(requestedBy);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPurpose(request.getPurpose());

        Reservation saved = reservationService.save(reservation);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    private ReservationResponse toResponse(Reservation r) {
        ReservationResponse resp = new ReservationResponse();
        resp.setId(r.getId());
        resp.setRoom(new RoomSummary(
                r.getRoom().getId(),
                r.getRoom().getName(),
                r.getRoom().getCode(),
                r.getRoom().getCapacity(),
                r.getRoom().getRoomType(),
                r.getRoom().getStatus()
        ));
        resp.setRequestedBy(new UserSummary(
                r.getRequestedBy().getId(),
                r.getRequestedBy().getPortalStaffId(),
                r.getRequestedBy().getName(),
                r.getRequestedBy().getRole()
        ));
        resp.setStartTime(r.getStartTime());
        resp.setEndTime(r.getEndTime());
        resp.setPurpose(r.getPurpose());
        resp.setStatus(r.getStatus());
        if (r.getApprovedBy() != null) {
            // approvedBy is stored as String ID, not a User entity
            resp.setApprovedBy(null);
        }
        resp.setApprovedAt(r.getApprovedAt());
        resp.setCreatedAt(r.getCreatedAt());
        resp.setUpdatedAt(r.getUpdatedAt());
        return resp;
    }
}
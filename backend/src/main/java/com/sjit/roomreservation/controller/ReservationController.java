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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new IllegalArgumentException("start_time must be before end_time");
        }

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Room not found"));

        // Get requesting user from authenticated JWT principal
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String portalStaffId = authentication.getName();
        User requestedBy = userRepository.findByPortalStaffId(portalStaffId)
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

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> getReservations(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String roomId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String portalStaffId = authentication.getName();
        User currentUser = userRepository.findByPortalStaffId(portalStaffId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found"));

        LocalDateTime fromDate = from != null ? LocalDateTime.parse(from) : null;
        LocalDateTime toDate = to != null ? LocalDateTime.parse(to) : null;

        List<Reservation> reservations;

        // Staff sees only their own reservations; HOD sees all
        if ("STAFF".equals(currentUser.getRole())) {
            reservations = reservationService.findByRequester(currentUser.getId());
            // Apply additional filters
            if (status != null) {
                reservations = reservations.stream().filter(r -> status.equals(r.getStatus())).toList();
            }
            if (roomId != null) {
                reservations = reservations.stream().filter(r -> roomId.equals(r.getRoom().getId())).toList();
            }
            if (fromDate != null) {
                reservations = reservations.stream().filter(r -> !r.getStartTime().isBefore(fromDate)).toList();
            }
            if (toDate != null) {
                reservations = reservations.stream().filter(r -> !r.getEndTime().isAfter(toDate)).toList();
            }
        } else {
            reservations = reservationService.findWithFilters(status, roomId, fromDate, toDate);
        }

        List<ReservationResponse> responses = reservations.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('HOD')")
    public ResponseEntity<ReservationResponse> approveReservation(@PathVariable String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String portalStaffId = authentication.getName();
        User hod = userRepository.findByPortalStaffId(portalStaffId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found"));

        Reservation approved = reservationService.approve(id, hod.getId());
        return ResponseEntity.ok(toResponse(approved));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('HOD')")
    public ResponseEntity<ReservationResponse> rejectReservation(@PathVariable String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String portalStaffId = authentication.getName();
        User hod = userRepository.findByPortalStaffId(portalStaffId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found"));

        Reservation rejected = reservationService.reject(id, hod.getId());
        return ResponseEntity.ok(toResponse(rejected));
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
            userRepository.findById(r.getApprovedBy()).ifPresent(user -> {
                resp.setApprovedBy(new UserSummary(
                        user.getId(),
                        user.getPortalStaffId(),
                        user.getName(),
                        user.getRole()
                ));
            });
        }
        resp.setApprovedAt(r.getApprovedAt());
        resp.setCreatedAt(r.getCreatedAt());
        resp.setUpdatedAt(r.getUpdatedAt());
        return resp;
    }
}
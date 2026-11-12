package com.sjit.roomreservation.controller;

import com.sjit.roomreservation.entity.Reservation;
import com.sjit.roomreservation.service.ReservationService;
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

    @PostMapping
    public ResponseEntity<?> createReservation(@RequestBody Reservation reservation) {
        // Validate start < end
        if (reservation.getStartTime() == null || reservation.getEndTime() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "start_time and end_time are required"));
        }
        if (!reservation.getStartTime().isBefore(reservation.getEndTime())) {
            return ResponseEntity.badRequest().body(Map.of("error", "start_time must be before end_time"));
        }

        try {
            Reservation saved = reservationService.save(reservation);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }
}
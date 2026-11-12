package com.sjit.roomreservation.service;

import com.sjit.roomreservation.entity.Reservation;
import com.sjit.roomreservation.exception.ResourceConflictException;
import com.sjit.roomreservation.repository.ReservationRepository;
import com.sjit.roomreservation.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;

    public ReservationService(ReservationRepository reservationRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
    }

    /** Save a reservation after checking for overlap with existing reservations. */
    @Transactional
    public Reservation save(Reservation reservation) {
        // Reject start times in the past
        if (reservation.getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("start_time cannot be in the past");
        }

        // Pessimistic lock on room row to prevent race conditions
        roomRepository.findByIdForUpdate(reservation.getRoom().getId());

        // Overlap check: PENDING + CONFIRMED (prevent double-booking even with pending requests)
        if (reservationRepository.existsOverlapByRoom(
                reservation.getRoom().getId(),
                reservation.getId(),
                reservation.getStartTime(),
                reservation.getEndTime())) {
            throw new ResourceConflictException(
                    "Room is already booked for the requested time range. Double-booking prevented.");
        }

        return reservationRepository.save(reservation);
    }

    /** Find reservations with optional filters. */
    public List<Reservation> findWithFilters(String status, String roomId, LocalDateTime from, LocalDateTime to) {
        return reservationRepository.findWithFilters(status, roomId, from, to);
    }

    /** Find reservations by requesting user. */
    public List<Reservation> findByRequester(String userId) {
        return reservationRepository.findByRequestedBy_Id(userId);
    }

    /** Approve a reservation (HOD only). Re-runs overlap check against other CONFIRMED reservations only. */
    @Transactional
    public Reservation approve(String reservationId, String approvedByUserId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Reservation not found"));

        if (!"PENDING".equals(reservation.getStatus())) {
            throw new ResourceConflictException("Reservation is not PENDING, cannot approve. Current status: " + reservation.getStatus());
        }

        // Pessimistic lock on room row
        roomRepository.findByIdForUpdate(reservation.getRoom().getId());

        // Overlap check: CONFIRMED only (at approve time, only confirmed reservations matter)
        if (reservationRepository.existsOverlapConfirmedByRoom(
                reservation.getRoom().getId(),
                reservation.getId(),
                reservation.getStartTime(),
                reservation.getEndTime())) {
            throw new ResourceConflictException(
                    "Room is already booked for the requested time range. Cannot approve due to conflict with confirmed reservation.");
        }

        reservation.setStatus("CONFIRMED");
        reservation.setApprovedBy(approvedByUserId);
        reservation.setApprovedAt(LocalDateTime.now());
        reservation.setUpdatedAt(LocalDateTime.now());
        return reservationRepository.save(reservation);
    }

    /** Reject a reservation (HOD only). */
    @Transactional
    public Reservation reject(String reservationId, String approvedByUserId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Reservation not found"));

        if (!"PENDING".equals(reservation.getStatus())) {
            throw new ResourceConflictException("Reservation is not PENDING, cannot reject. Current status: " + reservation.getStatus());
        }

        // Pessimistic lock on room row
        roomRepository.findByIdForUpdate(reservation.getRoom().getId());

        reservation.setStatus("REJECTED");
        reservation.setApprovedBy(approvedByUserId);
        reservation.setApprovedAt(LocalDateTime.now());
        reservation.setUpdatedAt(LocalDateTime.now());
        return reservationRepository.save(reservation);
    }
}
package com.sjit.roomreservation.service;

import com.sjit.roomreservation.dto.ReservationResponse;
import com.sjit.roomreservation.entity.Reservation;
import com.sjit.roomreservation.exception.ResourceConflictException;
import com.sjit.roomreservation.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;

    public ReservationService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    /** Save a reservation after checking for overlap with existing reservations. */
    @Transactional
    public Reservation save(Reservation reservation) {
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

    /** Approve a reservation (HOD only). Re-runs overlap check against other CONFIRMED reservations. */
    @Transactional
    public Reservation approve(String reservationId, String approvedByUserId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Reservation not found"));

        if (!"PENDING".equals(reservation.getStatus())) {
            throw new ResourceConflictException("Reservation is not PENDING, cannot approve. Current status: " + reservation.getStatus());
        }

        // Re-run overlap check against other CONFIRMED reservations
        if (reservationRepository.existsOverlapByRoom(
                reservation.getRoom().getId(),
                reservation.getId(),
                reservation.getStartTime(),
                reservation.getEndTime())) {
            throw new ResourceConflictException(
                    "Room is already booked for the requested time range. Cannot approve due to conflict.");
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

        reservation.setStatus("REJECTED");
        reservation.setApprovedBy(approvedByUserId);
        reservation.setApprovedAt(LocalDateTime.now());
        reservation.setUpdatedAt(LocalDateTime.now());
        return reservationRepository.save(reservation);
    }
}
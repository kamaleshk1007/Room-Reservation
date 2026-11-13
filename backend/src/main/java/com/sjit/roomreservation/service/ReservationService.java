package com.sjit.roomreservation.service;

import com.sjit.roomreservation.entity.Reservation;
import com.sjit.roomreservation.entity.User;
import com.sjit.roomreservation.exception.ResourceConflictException;
import com.sjit.roomreservation.repository.ReservationRepository;
import com.sjit.roomreservation.repository.RoomRepository;
import com.sjit.roomreservation.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public ReservationService(ReservationRepository reservationRepository, RoomRepository roomRepository,
                              UserRepository userRepository, AuditLogService auditLogService) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
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

        Reservation saved = reservationRepository.save(reservation);

        // Audit log
        auditLogService.log("Reservation", saved.getId(), "CREATE", reservation.getRequestedBy(),
                "Room: " + reservation.getRoom().getId() + ", " + reservation.getStartTime() + " to " + reservation.getEndTime());

        return saved;
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

        User hod = userRepository.findById(approvedByUserId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found"));

        reservation.setStatus("CONFIRMED");
        reservation.setApprovedBy(approvedByUserId);
        reservation.setApprovedAt(LocalDateTime.now());
        reservation.setUpdatedAt(LocalDateTime.now());
        Reservation saved = reservationRepository.save(reservation);

        // Audit log
        auditLogService.log("Reservation", saved.getId(), "APPROVE", hod,
                "Approved by HOD: " + hod.getPortalStaffId());

        return saved;
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

        User hod = userRepository.findById(approvedByUserId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found"));

        reservation.setStatus("REJECTED");
        reservation.setApprovedBy(approvedByUserId);
        reservation.setApprovedAt(LocalDateTime.now());
        reservation.setUpdatedAt(LocalDateTime.now());
        Reservation saved = reservationRepository.save(reservation);

        // Audit log
        auditLogService.log("Reservation", saved.getId(), "REJECT", hod,
                "Rejected by HOD: " + hod.getPortalStaffId());

        return saved;
    }

    /** Cancel a reservation (owner only, only PENDING or CONFIRMED future ones). */
    @Transactional
    public Reservation cancel(String reservationId, String cancelledByUserId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Reservation not found"));

        User cancelledBy = userRepository.findById(cancelledByUserId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User not found"));

        // Only owner can cancel
        if (!reservation.getRequestedBy().getId().equals(cancelledByUserId)) {
            throw new ResourceConflictException("Only the reservation owner can cancel it.");
        }

        // Only PENDING or CONFIRMED
        if (!"PENDING".equals(reservation.getStatus()) && !"CONFIRMED".equals(reservation.getStatus())) {
            throw new ResourceConflictException("Cannot cancel reservation with status: " + reservation.getStatus());
        }

        // Only future reservations
        if (reservation.getStartTime().isBefore(LocalDateTime.now())) {
            throw new ResourceConflictException("Cannot cancel a reservation that has already started.");
        }

        // Pessimistic lock on room row
        roomRepository.findByIdForUpdate(reservation.getRoom().getId());

        reservation.setStatus("CANCELLED");
        reservation.setUpdatedAt(LocalDateTime.now());
        Reservation saved = reservationRepository.save(reservation);

        // Audit log
        auditLogService.log("Reservation", saved.getId(), "CANCEL", cancelledBy,
                "Cancelled by owner: " + cancelledBy.getPortalStaffId());

        return saved;
    }
}
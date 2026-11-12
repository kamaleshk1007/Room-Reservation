package com.sjit.roomreservation.service;

import com.sjit.roomreservation.entity.Reservation;
import com.sjit.roomreservation.exception.ResourceConflictException;
import com.sjit.roomreservation.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
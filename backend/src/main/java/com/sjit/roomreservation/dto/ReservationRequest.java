package com.sjit.roomreservation.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Request DTO for creating a reservation.
 * The requesting user is taken from the authenticated JWT principal, not from the request body.
 */
public class ReservationRequest {

    @NotNull(message = "roomId is required")
    private String roomId;

    @NotNull(message = "startTime is required")
    private LocalDateTime startTime;

    @NotNull(message = "endTime is required")
    private LocalDateTime endTime;

    private String purpose;

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
}
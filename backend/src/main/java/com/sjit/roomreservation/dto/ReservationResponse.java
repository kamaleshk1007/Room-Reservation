package com.sjit.roomreservation.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for reservation information.
 * Never exposes internal entity proxies or JPA-managed fields.
 */
public class ReservationResponse {

    private String id;
    private RoomSummary room;
    private UserSummary requestedBy;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String purpose;
    private String status;
    private UserSummary approvedBy;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ReservationResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public RoomSummary getRoom() { return room; }
    public void setRoom(RoomSummary room) { this.room = room; }

    public UserSummary getRequestedBy() { return requestedBy; }
    public void setRequestedBy(UserSummary requestedBy) { this.requestedBy = requestedBy; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public UserSummary getApprovedBy() { return approvedBy; }
    public void setApprovedBy(UserSummary approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
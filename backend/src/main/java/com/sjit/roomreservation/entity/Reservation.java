package com.sjit.roomreservation.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Reservation entity representing a room booking request.
 * Mapped to the 'reservations' table.
 * Supports double-booking protection via trigger trg_prevent_room_overlap.
 */
@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @Column(name = "start_time", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime endTime;

    @Column(name = "purpose", length = 255)
    private String purpose;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "approved_by", length = 36)
    private String approvedBy;

    @Column(name = "approved_at", columnDefinition = "DATETIME(6)")
    private LocalDateTime approvedAt;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime updatedAt;

    // Default constructor (required by JPA)
    public Reservation() {
        this.id = UUID.randomUUID().toString();
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Full constructor
    public Reservation(Room room, User requestedBy, LocalDateTime startTime, LocalDateTime endTime, String purpose) {
        this.id = UUID.randomUUID().toString();
        this.room = room;
        this.requestedBy = requestedBy;
        this.startTime = startTime;
        this.endTime = endTime;
        this.purpose = purpose;
        this.status = "PENDING";
        this.approvedBy = null;
        this.approvedAt = null;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }

    public User getRequestedBy() { return requestedBy; }
    public void setRequestedBy(User requestedBy) { this.requestedBy = requestedBy; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Reservation{" +
                "id='" + id + '\'' +
                ", room=" + room.getName() +
                ", requestedBy=" + requestedBy.getName() +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", status='" + status + '\'' +
                "}";
    }
}
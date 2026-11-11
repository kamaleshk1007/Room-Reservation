package com.sjit.roomreservation.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * UsageSession entity representing actual room usage recording by Lab-incharge.
 * Mapped to the 'usage_sessions' table.
 * Records start/end times and duration of actual room usage for a reservation.
 */
@Entity
@Table(name = "usage_sessions")
public class UsageSession {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(name = "start_time", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime startTime;

    @Column(name = "end_time", columnDefinition = "DATETIME(6)")
    private LocalDateTime endTime;

    @Column(name = "actual_duration_minutes")
    private Integer actualDurationMinutes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_by", nullable = false)
    private User checkedBy;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime createdAt;

    // Default constructor (required by JPA)
    public UsageSession() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    // Full constructor
    public UsageSession(Reservation reservation, User checkedBy, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = UUID.randomUUID().toString();
        this.reservation = reservation;
        this.checkedBy = checkedBy;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Reservation getReservation() { return reservation; }
    public void setReservation(Reservation reservation) { this.reservation = reservation; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public Integer getActualDurationMinutes() { return actualDurationMinutes; }
    public void setActualDurationMinutes(Integer actualDurationMinutes) { this.actualDurationMinutes = actualDurationMinutes; }

    public User getCheckedBy() { return checkedBy; }
    public void setCheckedBy(User checkedBy) { this.checkedBy = checkedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "UsageSession{" +
                "id='" + id + '\'' +
                ", reservation=" + reservation.getId() +
                ", checkedBy=" + checkedBy.getPortalStaffId() +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", durationMin=" + actualDurationMinutes +
                "}";
    }
}
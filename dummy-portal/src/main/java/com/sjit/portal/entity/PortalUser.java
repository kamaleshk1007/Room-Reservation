package com.sjit.portal.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * College portal staff account. Stands in for the real portal user table.
 * staff_id is the stable identifier Room Reserve maps onto users.portal_staff_id.
 */
@Entity
@Table(name = "portal_users")
public class PortalUser {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "staff_id", nullable = false, unique = true, length = 50)
    private String staffId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "role", nullable = false, length = 50)
    private String role;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime createdAt;

    public PortalUser() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStaffId() { return staffId; }
    public void setStaffId(String staffId) { this.staffId = staffId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

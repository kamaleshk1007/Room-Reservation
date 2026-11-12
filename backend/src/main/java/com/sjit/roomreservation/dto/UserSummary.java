package com.sjit.roomreservation.dto;

/**
 * Response DTO for user information.
 * Exposes only safe fields — never passwordHash, email, or internal timestamps.
 */
public class UserSummary {

    private String id;
    private String portalStaffId;
    private String name;
    private String role;

    public UserSummary() {}

    public UserSummary(String id, String portalStaffId, String name, String role) {
        this.id = id;
        this.portalStaffId = portalStaffId;
        this.name = name;
        this.role = role;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPortalStaffId() { return portalStaffId; }
    public void setPortalStaffId(String portalStaffId) { this.portalStaffId = portalStaffId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
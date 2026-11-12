package com.sjit.roomreservation.dto;

/**
 * Response DTO for room information.
 * Exposes only safe fields — never internal timestamps.
 */
public class RoomSummary {

    private String id;
    private String name;
    private String code;
    private Integer capacity;
    private String roomType;
    private String status;

    public RoomSummary() {}

    public RoomSummary(String id, String name, String code, Integer capacity, String roomType, String status) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.capacity = capacity;
        this.roomType = roomType;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
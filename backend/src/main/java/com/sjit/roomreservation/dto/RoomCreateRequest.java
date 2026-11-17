package com.sjit.roomreservation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a room.
 * roomType is required because the database column is NOT NULL.
 * location is optional.
 */
public class RoomCreateRequest {

    @NotBlank(message = "name is required")
    @Size(max = 255, message = "name must be at most 255 characters")
    private String name;

    @NotBlank(message = "code is required")
    @Size(max = 50, message = "code must be at most 50 characters")
    private String code;

    @NotNull(message = "capacity is required")
    @Min(value = 1, message = "capacity must be at least 1")
    private Integer capacity;

    @NotBlank(message = "roomType is required")
    @Size(max = 100, message = "roomType must be at most 100 characters")
    private String roomType;

    @Size(max = 255, message = "location must be at most 255 characters")
    private String location;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}

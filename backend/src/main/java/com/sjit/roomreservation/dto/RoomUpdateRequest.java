package com.sjit.roomreservation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating a room's name, capacity and location.
 * code and roomType are immutable and are not part of this request.
 */
public class RoomUpdateRequest {

    @NotBlank(message = "name is required")
    @Size(max = 255, message = "name must be at most 255 characters")
    private String name;

    @NotNull(message = "capacity is required")
    @Min(value = 1, message = "capacity must be at least 1")
    private Integer capacity;

    @Size(max = 255, message = "location must be at most 255 characters")
    private String location;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}

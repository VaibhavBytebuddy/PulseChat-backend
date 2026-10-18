package com.pulsechat.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
    @NotBlank(message = "Room name is required")
    @Size(min = 3, max = 50, message = "Room name must be between 3 and 50 characters")
    String name,

    @Size(max = 200, message = "Description cannot exceed 200 characters")
    String description,

    String createdBy
) {}

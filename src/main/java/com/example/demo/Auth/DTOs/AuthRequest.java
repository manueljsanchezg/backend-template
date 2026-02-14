package com.example.demo.Auth.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequest(
        @NotBlank @Size(min = 4) String username,
        @NotBlank @Size(min = 4) String password,
        @NotBlank                String deviceId
) {
}

package com.example.demo.User.DTOs;

import com.example.demo.User.Role;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUser(
        @NotNull @Size(min = 4) String username,
        @NotNull @Size(min = 4) String password,
        @NotNull Role role
) {
}

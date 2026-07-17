package com.vbank.user_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(

        @NotBlank(message = "Username is required.")
        @Pattern(
                regexp = "^\\s*$|^[a-zA-Z0-9._-]{3,50}$",
                message = "Username must contain between 3 and 50 characters and may only contain letters, numbers, dots, underscores, and hyphens."
        )
        String username,

        @NotBlank(message = "Password is required.")
        @Pattern(
                regexp = "^\\s*$|^[\\s\\S]{8,100}$",
                message = "Password must contain between 8 and 100 characters."
        )
        String password,

        @NotBlank(message = "Email is required.")
        @Email(message = "Email must be valid.")
        @Size(max = 255, message = "Email is too long.")
        String email,

        @NotBlank(message = "First name is required.")
        @Size(max = 100, message = "First name is too long.")
        String firstName,

        @NotBlank(message = "Last name is required.")
        @Size(max = 100, message = "Last name is too long.")
        String lastName
) {
}
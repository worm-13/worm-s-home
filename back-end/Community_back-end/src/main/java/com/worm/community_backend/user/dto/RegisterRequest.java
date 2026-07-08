package com.worm.community_backend.user.dto;
import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
@Data
public class RegisterRequest {
    @NotBlank(message = "username is required")
    private String username;
    @NotBlank(message = "password is required")
    @Size(min = 6, message = "password must be at least 6 characters")
    private String password;
    @Email(message = "email format is invalid")
    private String email;
}

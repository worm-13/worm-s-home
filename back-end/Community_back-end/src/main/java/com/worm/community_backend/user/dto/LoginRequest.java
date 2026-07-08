package com.worm.community_backend.user.dto;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
@Data
public class LoginRequest {
    @NotBlank(message = "identifier is required(username/id/email)")
    private String identifier;
    @NotBlank(message = "password is required")
    private String password;
}

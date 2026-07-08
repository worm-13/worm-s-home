package com.worm.community_backend.user.dto;
import lombok.Data;
@Data
public class CreateUserRequest {
    private String username;
    private String password;
    private String nickname;
    private String email;
}

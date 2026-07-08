package com.worm.community_backend.user.vo;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String avatar;
    private String backgroundImage;
    private String bio;
    private Integer gender;
    private LocalDate birthday;
    private String location;
    private Integer followersCount;
    private Integer followingCount;
    private Integer postsCount;
    private Integer status;
    private String role;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}

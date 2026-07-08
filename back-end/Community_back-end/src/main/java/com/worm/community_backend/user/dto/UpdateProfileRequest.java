package com.worm.community_backend.user.dto;
import lombok.Data;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
@Data
public class UpdateProfileRequest {
    @Size(max = 50, message = "nickname too long")
    private String nickname;
    @Size(max = 500, message = "bio too long")
    private String bio;
    private Integer gender;
    private LocalDate birthday;
    @Size(max = 100, message = "location too long")
    private String location;
}

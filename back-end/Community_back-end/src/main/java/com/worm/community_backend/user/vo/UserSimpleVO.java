package com.worm.community_backend.user.vo;

import lombok.Data;

@Data
public class UserSimpleVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String bio;
}

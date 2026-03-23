package com.worm.community_backend.user.mapper;

import com.worm.community_backend.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
    User selectById(Long id);
}


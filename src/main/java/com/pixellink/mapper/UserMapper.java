package com.pixellink.mapper;

import com.pixellink.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    User findById(@Param("id") String id);
    int insert(User user);
}

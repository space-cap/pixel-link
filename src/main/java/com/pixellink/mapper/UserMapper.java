package com.pixellink.mapper;

import com.pixellink.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface UserMapper {
    User findById(@Param("id") String id);
    List<User> findAll();
    void updateRole(@Param("id") String id, @Param("role") String role);
    int insert(User user);
}

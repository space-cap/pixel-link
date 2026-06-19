package com.pixellink.mapper;

import com.pixellink.model.ApiKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ApiKeyMapper {
    void insert(ApiKey apiKey);
    ApiKey findByApiKey(@Param("apiKey") String apiKey);
    List<ApiKey> findByUserId(@Param("userId") String userId);
    void updateActiveStatus(@Param("id") String id, @Param("isActive") boolean isActive);
    void deleteById(@Param("id") String id);
}

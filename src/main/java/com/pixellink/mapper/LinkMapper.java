package com.pixellink.mapper;

import com.pixellink.model.Link;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface LinkMapper {
    Link findById(@Param("id") String id);
    Link findByShortCode(@Param("shortCode") String shortCode);
    List<Link> findByUserId(@Param("userId") String userId);
    int insert(Link link);
    int update(Link link);
    int delete(@Param("id") String id);
    int incrementClicksCount(@Param("id") String id);
    int deleteExpiredLinks(@Param("now") java.time.LocalDateTime now);
}

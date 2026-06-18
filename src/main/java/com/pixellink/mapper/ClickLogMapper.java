package com.pixellink.mapper;

import com.pixellink.model.ClickLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface ClickLogMapper {
    int insert(ClickLog clickLog);
    int countByLinkId(@Param("linkId") String linkId);
    
    // 통계용 집계 API
    List<Map<String, Object>> getClicksByDevice(@Param("linkId") String linkId);
    List<Map<String, Object>> getClicksByOs(@Param("linkId") String linkId);
    List<Map<String, Object>> getClicksByReferrer(@Param("linkId") String linkId);
    List<Map<String, Object>> getDailyClicks(@Param("linkId") String linkId);
}

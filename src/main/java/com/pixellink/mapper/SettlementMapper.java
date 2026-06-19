package com.pixellink.mapper;

import com.pixellink.model.Settlement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface SettlementMapper {
    List<Settlement> findByUserId(String userId);
    void insert(Settlement settlement);
    void updateStatus(@Param("id") String id, @Param("status") String status);
    void updateStatusByUserId(@Param("userId") String userId, @Param("status") String status);
    Integer sumAmountByUserId(String userId);
}


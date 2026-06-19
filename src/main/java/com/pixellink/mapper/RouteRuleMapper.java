package com.pixellink.mapper;

import com.pixellink.model.RouteRule;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface RouteRuleMapper {
    List<RouteRule> findByLinkId(String linkId);
    void insert(RouteRule rule);
    void deleteByLinkId(String linkId);
    void delete(String id);
}

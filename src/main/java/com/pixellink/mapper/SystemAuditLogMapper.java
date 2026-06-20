package com.pixellink.mapper;

import com.pixellink.model.SystemAuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SystemAuditLogMapper {
    int insert(SystemAuditLog log);
}

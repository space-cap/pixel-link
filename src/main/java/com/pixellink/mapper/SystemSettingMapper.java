package com.pixellink.mapper;

import com.pixellink.model.SystemSetting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SystemSettingMapper {
    SystemSetting findByKey(@Param("key") String key);
    void updateValue(@Param("key") String key, @Param("value") String value);
    void insert(@Param("key") String key, @Param("value") String value, @Param("description") String description);
    List<SystemSetting> findAll();
}

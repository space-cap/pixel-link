package com.pixellink.model;

import lombok.Data;

@Data
public class SystemSetting {
    private String settingKey;
    private String settingValue;
    private String description;
}

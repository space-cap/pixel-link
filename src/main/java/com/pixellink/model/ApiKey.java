package com.pixellink.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {
    private String id;
    private String userId;
    private String apiKey;
    private String name;
    private boolean isActive;
    private String createdAt;
}

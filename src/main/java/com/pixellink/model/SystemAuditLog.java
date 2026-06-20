package com.pixellink.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemAuditLog {
    private String id;
    private String eventType;
    private String actorId;
    private String ipAddress;
    private String userAgent;
    private String createdAt;
}

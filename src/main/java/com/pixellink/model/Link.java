package com.pixellink.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Link {
    private String id;
    private String userId;
    private String shortCode;
    private String defaultTargetUrl;
    private String title;
    private String description;
    private String ogImage;
    private String fbPixelId;
    private String gaTrackingId;
    private String customScript;
    private boolean isAdEnabled;
    private int adTimerSeconds;
    private boolean isPaywalled;
    private int price;
    private int clicksCount;
    private LocalDateTime createdAt;
}

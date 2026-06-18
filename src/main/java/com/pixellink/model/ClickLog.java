package com.pixellink.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ClickLog {
    private String id;
    private String linkId;
    private String userAgent;
    private String ipHash;
    private String referrer;
    private String deviceType;
    private String osType;
    private boolean isAdClicked;
    private boolean isConverted;
    private LocalDateTime timestamp;
}

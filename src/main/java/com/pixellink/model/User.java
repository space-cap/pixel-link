package com.pixellink.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private String id;
    private String email;
    private String subscriptionTier; // "FREE", "STARTER", "PREMIUM"
    private LocalDateTime subscriptionEndsAt;
    private String role; // "USER", "ADMIN"
}

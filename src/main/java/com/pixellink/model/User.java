package com.pixellink.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private String id;
    private String email;
    private String password;
    private String name;
    private String phone;
    private boolean termsAgreed;
    private String subscriptionTier; // "FREE", "STARTER", "PREMIUM"
    private LocalDateTime subscriptionEndsAt;
    private String role; // "USER", "ADMIN"
}

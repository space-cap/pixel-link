package com.pixellink.dto;

import com.pixellink.model.User;
import lombok.Getter;

import java.io.Serializable;

@Getter
public class SessionUser implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String email;
    private final String name;
    private final String subscriptionTier;
    private final String role;

    public SessionUser(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.name = user.getName();
        this.subscriptionTier = user.getSubscriptionTier();
        this.role = user.getRole();
    }
}

package com.api.monitor.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String name;

    private String picture;

    @Column(name = "subscription_tier")
    private String subscriptionTier = "FREE";

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Setters used by CustomOAuth2UserService (explicit for reliability with Lombok)
    public void setEmail(String email) {
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }
}
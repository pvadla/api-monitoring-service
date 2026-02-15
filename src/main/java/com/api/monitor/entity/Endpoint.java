package com.api.monitor.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "endpoints")
public class Endpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String url;

    @Column(name = "check_interval")
    private Integer checkInterval = 5; // minutes

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_up")
    private Boolean isUp = true;

    @Column(name = "last_checked")
    private LocalDateTime lastChecked;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
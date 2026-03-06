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
@Table(name = "heartbeat_monitors")
public class HeartbeatMonitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    /** Unique token used in the heartbeat URL. */
    @Column(name = "token", unique = true, nullable = false, length = 64)
    private String token;

    /** Expected interval between pings, in minutes. */
    @Column(name = "expected_interval_minutes")
    private Integer expectedIntervalMinutes = 5;

    @Column(name = "last_ping_at")
    private LocalDateTime lastPingAt;

    @Column(name = "last_notified_at")
    private LocalDateTime lastNotifiedAt;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}


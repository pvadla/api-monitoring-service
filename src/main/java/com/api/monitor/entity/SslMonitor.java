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
@Table(name = "ssl_monitors")
public class SslMonitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    /** Hostname to check, e.g. "example.com" or "api.example.com". No scheme prefix. */
    @Column(nullable = false)
    private String domain;

    /** TLS port, almost always 443. */
    @Column(nullable = false)
    private int port = 443;

    /** Alert when the cert expires within this many days (user-configurable, default 30). */
    @Column(name = "alert_days_threshold", nullable = false)
    private int alertDaysThreshold = 30;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /** Whether this monitor is visible on the public status page. */
    @Column(name = "show_on_status_page", nullable = false)
    private Boolean showOnStatusPage = true;

    /**
     * Current cert health.
     * null  = never checked yet.
     * true  = cert valid and daysLeft > alertDaysThreshold.
     * false = cert expired, TLS error, or daysLeft <= alertDaysThreshold.
     */
    @Column(name = "is_up")
    private Boolean isUp;

    /** Expiry date from the leaf certificate, null if never successfully checked. */
    @Column(name = "ssl_expires_at")
    private LocalDateTime sslExpiresAt;

    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    /** Guards re-alert: do not send another expiry email until this is cleared or renewed. */
    @Column(name = "last_notified_at")
    private LocalDateTime lastNotifiedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

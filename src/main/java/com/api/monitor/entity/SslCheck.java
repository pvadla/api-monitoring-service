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

@Entity
@Table(name = "ssl_checks")
public class SslCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ssl_monitor_id", nullable = false)
    private SslMonitor sslMonitor;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt = LocalDateTime.now();

    /** true = cert healthy; false = expiring/expired/TLS error */
    @Column(name = "is_up", nullable = false)
    private Boolean isUp;

    /** Days until cert expiry at check time; null when the TLS handshake itself failed. */
    @Column(name = "days_until_expiry")
    private Integer daysUntilExpiry;

    /** Non-null when the TLS connection or cert parse failed. */
    @Column(name = "error_message", length = 512)
    private String errorMessage;

    // ── Getters ──────────────────────────────────────────

    public Long getId() { return id; }
    public SslMonitor getSslMonitor() { return sslMonitor; }
    public LocalDateTime getCheckedAt() { return checkedAt; }
    public Boolean getIsUp() { return isUp; }
    public Integer getDaysUntilExpiry() { return daysUntilExpiry; }
    public String getErrorMessage() { return errorMessage; }

    // ── Setters ──────────────────────────────────────────

    public void setId(Long id) { this.id = id; }
    public void setSslMonitor(SslMonitor sslMonitor) { this.sslMonitor = sslMonitor; }
    public void setCheckedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; }
    public void setIsUp(Boolean isUp) { this.isUp = isUp; }
    public void setDaysUntilExpiry(Integer daysUntilExpiry) { this.daysUntilExpiry = daysUntilExpiry; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}

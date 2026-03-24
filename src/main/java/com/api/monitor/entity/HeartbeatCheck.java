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
@Table(name = "heartbeat_checks")
public class HeartbeatCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "heartbeat_monitor_id", nullable = false)
    private HeartbeatMonitor heartbeatMonitor;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt = LocalDateTime.now();

    /** true = ping received within interval; false = overdue/missed */
    @Column(name = "is_up", nullable = false)
    private Boolean isUp;

    // ── Getters ──────────────────────────────────────────

    public Long getId() { return id; }

    public HeartbeatMonitor getHeartbeatMonitor() { return heartbeatMonitor; }

    public LocalDateTime getCheckedAt() { return checkedAt; }

    public Boolean getIsUp() { return isUp; }

    // ── Setters ──────────────────────────────────────────

    public void setId(Long id) { this.id = id; }

    public void setHeartbeatMonitor(HeartbeatMonitor heartbeatMonitor) { this.heartbeatMonitor = heartbeatMonitor; }

    public void setCheckedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; }

    public void setIsUp(Boolean isUp) { this.isUp = isUp; }
}

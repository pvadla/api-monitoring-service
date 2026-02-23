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
@Table(name = "endpoint_checks")
public class EndpointCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "endpoint_id", nullable = false)
    private Endpoint endpoint;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt = LocalDateTime.now();

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "is_up", nullable = false)
    private Boolean isUp;

    @Column(name = "error_message")
    private String errorMessage;

    // ── Getters ──────────────────────────────────────────

    public Long getId() { return id; }

    public Endpoint getEndpoint() { return endpoint; }

    public LocalDateTime getCheckedAt() { return checkedAt; }

    public Long getResponseTimeMs() { return responseTimeMs; }

    public Integer getStatusCode() { return statusCode; }

    public Boolean getIsUp() { return isUp; }

    public String getErrorMessage() { return errorMessage; }

    // ── Setters ──────────────────────────────────────────

    public void setId(Long id) { this.id = id; }

    public void setEndpoint(Endpoint endpoint) { this.endpoint = endpoint; }

    public void setCheckedAt(LocalDateTime checkedAt) { this.checkedAt = checkedAt; }

    public void setResponseTimeMs(Long responseTimeMs) { this.responseTimeMs = responseTimeMs; }

    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }

    public void setIsUp(Boolean isUp) { this.isUp = isUp; }

    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
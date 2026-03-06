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

    @Column(name = "failure_count")
    private Integer failureCount = 0;

    @Column(name = "last_checked")
    private LocalDateTime lastChecked;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Whether to show this endpoint on the public status page. */
    @Column(name = "show_on_status_page")
    private Boolean showOnStatusPage = true;

    /** Optional: response body must contain this text to be considered UP. */
    @Column(name = "expected_body_substring", length = 512)
    private String expectedBodySubstring;

    /** SSL certificate expiry (for HTTPS endpoints), if known. */
    @Column(name = "ssl_expires_at")
    private LocalDateTime sslExpiresAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getCheckInterval() {
        return checkInterval;
    }

    public void setCheckInterval(Integer checkInterval) {
        this.checkInterval = checkInterval;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsUp() {
        return isUp;
    }

    public void setIsUp(Boolean isUp) {
        this.isUp = isUp;
    }

    public Integer getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(Integer failureCount) {
        this.failureCount = failureCount;
    }

    public LocalDateTime getLastChecked() {
        return lastChecked;
    }

    public void setLastChecked(LocalDateTime lastChecked) {
        this.lastChecked = lastChecked;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getShowOnStatusPage() {
        return showOnStatusPage;
    }

    public void setShowOnStatusPage(Boolean showOnStatusPage) {
        this.showOnStatusPage = showOnStatusPage;
    }

    public String getExpectedBodySubstring() {
        return expectedBodySubstring;
    }

    public void setExpectedBodySubstring(String expectedBodySubstring) {
        this.expectedBodySubstring = expectedBodySubstring;
    }

    public LocalDateTime getSslExpiresAt() {
        return sslExpiresAt;
    }

    public void setSslExpiresAt(LocalDateTime sslExpiresAt) {
        this.sslExpiresAt = sslExpiresAt;
    }
}
package com.api.monitor.service;

import java.net.UnknownHostException;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.api.monitor.entity.Endpoint;
import com.api.monitor.entity.EndpointCheck;
import com.api.monitor.entity.HeartbeatCheck;
import com.api.monitor.entity.Incident;
import com.api.monitor.entity.Incident.IncidentStatus;
import com.api.monitor.entity.HeartbeatMonitor;
import com.api.monitor.entity.SslCheck;
import com.api.monitor.entity.SslMonitor;
import com.api.monitor.repository.EndpointCheckRepository;
import com.api.monitor.repository.EndpointRepository;
import com.api.monitor.repository.HeartbeatCheckRepository;
import com.api.monitor.repository.HeartbeatMonitorRepository;
import com.api.monitor.repository.IncidentRepository;
import com.api.monitor.repository.SslCheckRepository;
import com.api.monitor.repository.SslMonitorRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final EndpointRepository endpointRepository;
    private final EndpointCheckRepository endpointCheckRepository;
    private final IncidentRepository incidentRepository;
    private final HeartbeatMonitorRepository heartbeatMonitorRepository;
    private final HeartbeatCheckRepository heartbeatCheckRepository;
    private final SslMonitorRepository sslMonitorRepository;
    private final SslCheckRepository sslCheckRepository;
    private final WebClient webClient;
    private final EmailNotificationService emailNotificationService;

    // ─────────────────────────────────────────────────────
    //  Scheduler: fires every 60 seconds
    //  Each endpoint is only actually checked when its
    //  configured interval (e.g. 5 min) has elapsed.
    // ─────────────────────────────────────────────────────
    @Scheduled(fixedDelay = 60_000)
    public void runChecks() {
        List<Endpoint> due = endpointRepository.findAll()
                .stream()
                .filter(ep -> Boolean.TRUE.equals(ep.getIsActive()))
                .filter(this::isDue)
                .toList();

        if (due.isEmpty()) {
            log.debug("No endpoints due for checking.");
            return;
        }

        log.info("Checking {} endpoint(s)...", due.size());
        due.forEach(this::checkEndpoint);
    }

    // ─────────────────────────────────────────────────────
    //  Heartbeat monitors: check for missed pings every 60 s.
    //  • Uses lastPingAt if available, otherwise createdAt,
    //    so brand-new monitors also get flagged when overdue.
    //  • Updates isUp on the entity every cycle.
    //  • Creates one incident per missed window + sends email.
    //  • Incident is resolved (and recovery email sent) when
    //    the next ping arrives via resolveHeartbeatIncident().
    // ─────────────────────────────────────────────────────
    @Scheduled(fixedDelay = 60_000)
    public void runHeartbeatChecks() {
        List<HeartbeatMonitor> monitors = heartbeatMonitorRepository.findByIsActiveTrue();
        if (monitors.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (HeartbeatMonitor hb : monitors) {
            int interval = (hb.getExpectedIntervalMinutes() != null && hb.getExpectedIntervalMinutes() > 0)
                    ? hb.getExpectedIntervalMinutes()
                    : 5;

            // Use last ping if available, fall back to creation time for brand-new monitors
            LocalDateTime baseline = hb.getLastPingAt() != null ? hb.getLastPingAt() : hb.getCreatedAt();
            if (baseline == null) {
                continue;
            }

            // Deadline = baseline + interval + 1 minute grace
            LocalDateTime deadline = baseline.plusMinutes(interval + 1L);
            boolean overdue = now.isAfter(deadline);

            // ── Record check history row (every scheduler tick) ───
            HeartbeatCheck checkRow = new HeartbeatCheck();
            checkRow.setHeartbeatMonitor(hb);
            checkRow.setCheckedAt(now);
            checkRow.setIsUp(!overdue);
            heartbeatCheckRepository.save(checkRow);

            if (overdue) {
                // ── Mark DOWN if not already ──────────────────────
                if (!Boolean.FALSE.equals(hb.getIsUp())) {
                    hb.setIsUp(false);
                    heartbeatMonitorRepository.save(hb);
                }

                LocalDateTime lastNotified = hb.getLastNotifiedAt();
                // Only open a new incident/email once per missed window
                if (lastNotified == null || lastNotified.isBefore(deadline)) {
                    boolean hasOpenIncident = incidentRepository
                            .findFirstByHeartbeatMonitorAndResolvedAtIsNullOrderByStartedAtDesc(hb)
                            .isPresent();
                    Incident incident = null;
                    if (!hasOpenIncident) {
                        incident = new Incident();
                        incident.setUser(hb.getUser());
                        incident.setHeartbeatMonitor(hb);
                        incident.setAutoGenerated(true);
                        incident.setTitle("Heartbeat missed: " + hb.getName());
                        incident.setDescription("No ping received within the expected interval of "
                                + interval + " minute(s). Last ping: "
                                + (hb.getLastPingAt() != null ? hb.getLastPingAt().toString() : "never") + ".");
                        incident.setFailureReason("Heartbeat missed");
                        incident.setStatus(Incident.IncidentStatus.INVESTIGATING);
                        incident.setStartedAt(now);
                        incidentRepository.save(incident);
                        log.warn("🔴 HEARTBEAT INCIDENT created: '{}' (id={}) — no ping in {} min. Incident#{}",
                                hb.getName(), hb.getId(), interval, incident.getId());
                    }
                    try {
                        emailNotificationService.sendHeartbeatMissedEmail(hb, incident);
                    } catch (Exception ex) {
                        log.error("Failed to send heartbeat missed email for monitor {}: {}", hb.getId(), ex.getMessage(), ex);
                    }
                    hb.setLastNotifiedAt(now);
                    heartbeatMonitorRepository.save(hb);
                }
            } else {
                // ── Within window: mark UP if not already ─────────
                if (!Boolean.TRUE.equals(hb.getIsUp())) {
                    hb.setIsUp(true);
                    heartbeatMonitorRepository.save(hb);
                }
            }
        }
    }

    /**
     * Resolves any open heartbeat incident for the given monitor and optionally
     * sends a recovery email. Called when a ping arrives.
     */
    public void resolveHeartbeatIncident(HeartbeatMonitor hb) {
        incidentRepository
                .findFirstByHeartbeatMonitorAndResolvedAtIsNullOrderByStartedAtDesc(hb)
                .ifPresent(incident -> {
                    LocalDateTime now = LocalDateTime.now();
                    incident.setResolvedAt(now);
                    incident.setStatus(Incident.IncidentStatus.RESOLVED);
                    long durationMinutes = ChronoUnit.MINUTES.between(incident.getStartedAt(), now);
                    incident.setDowntimeDurationMinutes(durationMinutes);
                    incidentRepository.save(incident);
                    log.info("🟢 HEARTBEAT INCIDENT resolved: '{}' (id={}) — downtime {} minutes. Incident#{}",
                            hb.getName(), hb.getId(), durationMinutes, incident.getId());
                    try {
                        emailNotificationService.sendHeartbeatRecoveryEmail(hb, incident);
                    } catch (Exception ex) {
                        log.error("Failed to send heartbeat recovery email for monitor {}: {}", hb.getId(), ex.getMessage(), ex);
                    }
                });
    }

    // ─────────────────────────────────────────────────────
    //  Due check: has enough time passed since last check?
    // ─────────────────────────────────────────────────────
    private boolean isDue(Endpoint ep) {
        if (ep.getLastChecked() == null) {
            return true;
        }
        LocalDateTime nextCheck = ep.getLastChecked()
                .plusMinutes(ep.getCheckInterval());
        return LocalDateTime.now().isAfter(nextCheck);
    }

    // ─────────────────────────────────────────────────────
    //  Core check logic for a single endpoint
    // ─────────────────────────────────────────────────────
    private void checkEndpoint(Endpoint ep) {
        boolean wasUp = Boolean.TRUE.equals(ep.getIsUp());

        // Perform HTTP + content check and capture timing + status
        CheckResult result = performHttpCheck(ep);

        // Update SSL certificate info for HTTPS endpoints
        updateSslInfo(ep);

        // Update failure count
        if (result.isUp()) {
            ep.setFailureCount(0);
        } else {
            ep.setFailureCount(ep.getFailureCount() == null ? 1 : ep.getFailureCount() + 1);
        }

        ep.setIsUp(result.isUp());
        ep.setLastChecked(LocalDateTime.now());
        endpointRepository.save(ep);

        // ── Save check record for history graph ──────────
        EndpointCheck check = new EndpointCheck();
        check.setEndpoint(ep);
        check.setCheckedAt(ep.getLastChecked());
        check.setIsUp(result.isUp());
        check.setResponseTimeMs(result.responseTimeMs());
        check.setStatusCode(result.statusCode());
        check.setErrorMessage(result.errorMessage());
        endpointCheckRepository.save(check);

        // ── Auto-incident: create on failure, resolve on recovery ──
        if (!result.isUp() && ep.getFailureCount() >= 2) {
            boolean hasOpenIncident = incidentRepository
                    .findFirstByEndpointAndResolvedAtIsNullOrderByStartedAtDesc(ep)
                    .isPresent();
            if (!hasOpenIncident) {
                handleDownAlert(ep, result);
            }
        } else if (!wasUp && result.isUp()) {
            handleRecoveryAlert(ep);
        }
    }

    // ─────────────────────────────────────────────────────
    //  HTTP check — returns CheckResult with timing info
    //  Uses WebClient with a 10-second timeout.
    //  4xx/5xx → DOWN, exception (timeout, DNS fail) → DOWN
    // ─────────────────────────────────────────────────────
    private CheckResult performHttpCheck(Endpoint ep) {
        String url = ep.getUrl();
        long start = System.currentTimeMillis();
        try {
            var response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .onStatus(
                            httpStatus -> httpStatus.is4xxClientError()
                                    || httpStatus.is5xxServerError(),
                            clientResponse -> clientResponse.createException())
                    .toEntity(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            long elapsed = System.currentTimeMillis() - start;
            int statusCode = response != null ? response.getStatusCode().value() : 0;
            String body = response != null && response.getBody() != null ? response.getBody() : "";

            // HTTP status OK?
            if (statusCode >= 400) {
                log.warn("⚠️  {} → HTTP {} ({}ms)", url, statusCode, elapsed);
                return new CheckResult(false, elapsed, statusCode,
                        "HTTP " + statusCode, statusCode >= 500 ? "HTTP 5xx" : "HTTP 4xx");
            }

            // Optional body assertion
            String expected = ep.getExpectedBodySubstring();
            if (expected != null && !expected.isBlank() && !body.contains(expected)) {
                log.warn("⚠️  {} → BODY ASSERTION FAILED ({}ms) – expected text not found", url, elapsed);
                return new CheckResult(false, elapsed, statusCode,
                        "Expected to find \"" + expected + "\" in response body.",
                        "Body assertion failed");
            }

            log.debug("✅ {} → HTTP {} ({}ms)", url, statusCode, elapsed);
            return new CheckResult(true, elapsed, statusCode, null, null);

        } catch (WebClientResponseException ex) {
            long elapsed = System.currentTimeMillis() - start;
            int code = ex.getStatusCode().value();
            String reason = code >= 500 ? "HTTP 5xx" : "HTTP 4xx";
            String msg = code + " " + ex.getStatusText();
            log.warn("⚠️  {} → HTTP {} ({})", url, code, ex.getStatusText());
            return new CheckResult(false, elapsed, code, msg, reason);

        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - start;
            String reason = classifyFailureReason(ex);
            String errMsg = ex.getMessage();
            log.warn("🔴 {} → FAILED ({}): {}", url, reason, errMsg);
            return new CheckResult(false, elapsed, null, errMsg, reason);
        }
    }

    private static String classifyFailureReason(Throwable ex) {
        if (ex == null) {
            return "Unknown";
        }
        if (ex instanceof UnknownHostException) {
            return "DNS failure";
        }
        String raw = ex.getMessage();
        String msg = raw != null ? raw.toLowerCase() : "";
        if (msg.contains("timeout") || msg.contains("timed out")) {
            return "Timeout";
        }
        if (msg.contains("connection refused") || msg.contains("connection reset")) {
            return "Connection refused";
        }
        if (msg.contains("name or service not known") || msg.contains("nodename nor servname")) {
            return "DNS failure";
        }
        if (msg.contains("connection") && msg.contains("refused")) {
            return "Connection refused";
        }
        return ex.getClass().getSimpleName();
    }

    // ─────────────────────────────────────────────────────
    //  Auto-incident: create on failure, resolve on recovery
    // ─────────────────────────────────────────────────────
    private void handleDownAlert(Endpoint ep, CheckResult result) {
        Incident incident = new Incident();
        incident.setUser(ep.getUser());
        incident.setEndpoint(ep);
        incident.setAutoGenerated(true);
        incident.setTitle("Endpoint down: " + ep.getName());
        incident.setDescription(result.errorMessage() != null ? result.errorMessage() : result.failureReason());
        incident.setFailureReason(result.failureReason());
        incident.setStatus(IncidentStatus.INVESTIGATING);
        incident.setStartedAt(LocalDateTime.now());
        incidentRepository.save(incident);
        log.info("Incident created: {}", incident);

        log.warn("🔴 INCIDENT created: '{}' ({}) — {} [failures: {}]. Incident#{}",
                ep.getName(), ep.getUrl(), result.failureReason(), ep.getFailureCount(), incident.getId());
        try {
            emailNotificationService.sendEndpointDownEmail(
                    ep.getUser(),
                    ep,
                    incident,
                    result.failureReason(),
                    result.errorMessage(),
                    result.statusCode(),
                    result.responseTimeMs());
        } catch (Exception ex) {
            log.error("Failed to send down alert email for endpoint {}: {}", ep.getId(), ex.getMessage(), ex);
        }
    }

    private void handleRecoveryAlert(Endpoint ep) {
        incidentRepository
                .findFirstByEndpointAndResolvedAtIsNullOrderByStartedAtDesc(ep)
                .ifPresent(incident -> {
                    LocalDateTime now = LocalDateTime.now();
                    incident.setResolvedAt(now);
                    incident.setStatus(IncidentStatus.RESOLVED);
                    long durationMinutes = ChronoUnit.MINUTES.between(incident.getStartedAt(), now);
                    incident.setDowntimeDurationMinutes(durationMinutes);
                    incidentRepository.save(incident);

                    log.info("🟢 INCIDENT resolved: '{}' ({}) — downtime {} minutes. Incident#{}",
                            ep.getName(), ep.getUrl(), durationMinutes, incident.getId());
                    try {
                        emailNotificationService.sendEndpointRecoveryEmail(ep.getUser(), ep, incident);
                    } catch (Exception ex) {
                        log.error("Failed to send recovery email for endpoint {}: {}", ep.getId(), ex.getMessage(), ex);
                    }
                });
    }

    // ─────────────────────────────────────────────────────
    //  Shared SSL check primitive
    // ─────────────────────────────────────────────────────

    /** Result of a TLS handshake against a host:port. */
    record SslCheckResult(LocalDateTime expiryDate, long daysLeft, String errorMessage) {
        boolean isUp(int alertDaysThreshold) {
            return errorMessage == null && daysLeft > alertDaysThreshold;
        }
    }

    /**
     * Opens a TLS socket to {@code host:port} and reads the leaf-certificate expiry.
     * Returns a result with a non-null {@code errorMessage} if the handshake fails.
     */
    private SslCheckResult performSslCheck(String host, int port) {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, null, null);
            SSLSocketFactory factory = context.getSocketFactory();

            try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port)) {
                socket.setSoTimeout(10_000);
                socket.startHandshake();
                SSLSession session = socket.getSession();
                Certificate[] certs = session.getPeerCertificates();
                if (certs.length > 0 && certs[0] instanceof X509Certificate x509) {
                    var expiryInstant = x509.getNotAfter().toInstant();
                    LocalDateTime expiry = LocalDateTime.ofInstant(expiryInstant, java.time.ZoneId.systemDefault());
                    long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), expiry);
                    return new SslCheckResult(expiry, daysLeft, null);
                }
                return new SslCheckResult(null, 0, "No certificates in chain");
            }
        } catch (Exception ex) {
            return new SslCheckResult(null, 0, ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────
    //  SSL certificate monitoring for HTTP endpoints (embedded check)
    // ─────────────────────────────────────────────────────
    private void updateSslInfo(Endpoint ep) {
        String url = ep.getUrl();
        if (url == null || !url.toLowerCase().startsWith("https://")) {
            return;
        }
        try {
            URL parsed = new URL(url);
            String host = parsed.getHost();
            int port = parsed.getPort() > 0 ? parsed.getPort() : 443;

            SslCheckResult result = performSslCheck(host, port);
            if (result.errorMessage() != null) {
                log.debug("Failed to update SSL info for {}: {}", url, result.errorMessage());
                return;
            }

            ep.setSslExpiresAt(result.expiryDate());
            endpointRepository.save(ep);

            long daysLeft = result.daysLeft();
            if (daysLeft >= 0 && daysLeft <= 14) {
                boolean hasOpenIncident = incidentRepository
                        .findFirstByEndpointAndResolvedAtIsNullOrderByStartedAtDesc(ep)
                        .isPresent();
                if (!hasOpenIncident) {
                    Incident incident = new Incident();
                    incident.setUser(ep.getUser());
                    incident.setEndpoint(ep);
                    incident.setAutoGenerated(true);
                    incident.setTitle("SSL certificate expiring soon: " + ep.getName());
                    incident.setDescription("The SSL certificate for this endpoint expires in " + daysLeft + " days.");
                    incident.setFailureReason("SSL certificate expiry");
                    incident.setStatus(Incident.IncidentStatus.INVESTIGATING);
                    incident.setStartedAt(LocalDateTime.now());
                    incidentRepository.save(incident);

                    log.warn("🔒 SSL expiry incident created for endpoint {} (expires in {} days)", ep.getId(), daysLeft);
                    try {
                        emailNotificationService.sendSslExpiryEmail(ep.getUser(), ep, result.expiryDate(), daysLeft);
                    } catch (Exception ex) {
                        log.error("Failed to send SSL expiry email for endpoint {}: {}", ep.getId(), ex.getMessage(), ex);
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("Failed to update SSL info for {}: {}", url, ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────
    //  Scheduler: standalone SSL certificate monitors
    //  Fires every 30 minutes; each monitor is re-checked
    //  only when lastCheckedAt is null or > 6 hours ago.
    // ─────────────────────────────────────────────────────
    @Scheduled(fixedDelay = 1_800_000)
    public void runSslMonitorChecks() {
        List<SslMonitor> monitors = sslMonitorRepository.findByIsActiveTrue();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusHours(6);

        for (SslMonitor monitor : monitors) {
            if (monitor.getLastCheckedAt() != null && monitor.getLastCheckedAt().isAfter(cutoff)) {
                continue; // checked recently enough
            }
            try {
                checkSslMonitor(monitor, now);
            } catch (Exception ex) {
                log.error("Unhandled error checking SSL monitor {}: {}", monitor.getId(), ex.getMessage(), ex);
            }
        }
    }

    private void checkSslMonitor(SslMonitor monitor, LocalDateTime now) {
        SslCheckResult result = performSslCheck(monitor.getDomain(), monitor.getPort());
        boolean up = result.isUp(monitor.getAlertDaysThreshold());

        // Update monitor fields
        monitor.setIsUp(up);
        monitor.setSslExpiresAt(result.expiryDate());
        monitor.setLastCheckedAt(now);

        // Persist a check row for sparkline history
        SslCheck check = new SslCheck();
        check.setSslMonitor(monitor);
        check.setCheckedAt(now);
        check.setIsUp(up);
        check.setDaysUntilExpiry(result.errorMessage() == null ? (int) result.daysLeft() : null);
        check.setErrorMessage(result.errorMessage());
        sslCheckRepository.save(check);

        if (!up) {
            // Open an incident if none is open yet and we haven't notified recently
            boolean hasOpenIncident = incidentRepository
                    .findFirstBySslMonitorAndResolvedAtIsNullOrderByStartedAtDesc(monitor)
                    .isPresent();
            LocalDateTime lastNotified = monitor.getLastNotifiedAt();
            boolean shouldNotify = lastNotified == null || lastNotified.isBefore(now.minusHours(24));

            if (!hasOpenIncident && shouldNotify) {
                Incident incident = new Incident();
                incident.setUser(monitor.getUser());
                incident.setSslMonitor(monitor);
                incident.setAutoGenerated(true);

                if (result.errorMessage() != null) {
                    incident.setTitle("SSL check failed: " + monitor.getName());
                    incident.setDescription("TLS handshake error: " + result.errorMessage());
                    incident.setFailureReason("TLS error");
                } else {
                    long days = result.daysLeft();
                    if (days < 0) {
                        incident.setTitle("SSL certificate expired: " + monitor.getName());
                        incident.setDescription("The SSL certificate for " + monitor.getDomain() + " has expired.");
                        incident.setFailureReason("SSL certificate expired");
                    } else {
                        incident.setTitle("SSL certificate expiring soon: " + monitor.getName());
                        incident.setDescription("The SSL certificate for " + monitor.getDomain()
                                + " expires in " + days + " day(s).");
                        incident.setFailureReason("SSL certificate expiry");
                    }
                }
                incident.setStatus(IncidentStatus.INVESTIGATING);
                incident.setStartedAt(now);
                incidentRepository.save(incident);
                monitor.setLastNotifiedAt(now);
                log.warn("🔒 SSL incident created for monitor {} ({}:{})", monitor.getId(), monitor.getDomain(), monitor.getPort());

                try {
                    emailNotificationService.sendSslMonitorExpiryEmail(monitor.getUser(), monitor,
                            result.daysLeft(), result.errorMessage());
                } catch (Exception ex) {
                    log.error("Failed to send SSL monitor expiry email for monitor {}: {}", monitor.getId(), ex.getMessage(), ex);
                }
            }
        } else {
            // Cert is healthy — resolve any open incident and send recovery email
            incidentRepository
                    .findFirstBySslMonitorAndResolvedAtIsNullOrderByStartedAtDesc(monitor)
                    .ifPresent(incident -> {
                        incident.setResolvedAt(now);
                        incident.setStatus(IncidentStatus.RESOLVED);
                        long durationMinutes = ChronoUnit.MINUTES.between(incident.getStartedAt(), now);
                        incident.setDowntimeDurationMinutes(durationMinutes);
                        incidentRepository.save(incident);
                        monitor.setLastNotifiedAt(null); // reset so next issue re-alerts
                        log.info("🟢 SSL incident resolved for monitor {} ({}:{})", monitor.getId(), monitor.getDomain(), monitor.getPort());

                        try {
                            emailNotificationService.sendSslMonitorRecoveryEmail(monitor.getUser(), monitor, incident);
                        } catch (Exception ex) {
                            log.error("Failed to send SSL monitor recovery email for monitor {}: {}", monitor.getId(), ex.getMessage(), ex);
                        }
                    });
        }

        sslMonitorRepository.save(monitor);
    }

    /**
     * Runs a TLS check immediately (e.g. right after creating a monitor) so the dashboard
     * does not stay in "pending" until the next scheduled run.
     */
    public void checkSslMonitorNow(SslMonitor monitor) {
        checkSslMonitor(monitor, LocalDateTime.now());
    }

    // ─────────────────────────────────────────────────────
    //  Check result with failure classification for incidents
    // ─────────────────────────────────────────────────────
    static record CheckResult(
            boolean isUp,
            long responseTimeMs,
            Integer statusCode,
            String errorMessage,
            String failureReason) {

    }
}

package com.api.monitor.service;

import java.net.UnknownHostException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.api.monitor.entity.Endpoint;
import com.api.monitor.entity.EndpointCheck;
import com.api.monitor.entity.Incident;
import com.api.monitor.entity.Incident.IncidentStatus;
import com.api.monitor.repository.EndpointCheckRepository;
import com.api.monitor.repository.EndpointRepository;
import com.api.monitor.repository.IncidentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final EndpointRepository endpointRepository;
    private final EndpointCheckRepository endpointCheckRepository;
    private final IncidentRepository incidentRepository;
    private final WebClient webClient;

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

        // Perform HTTP check and capture timing + status
        CheckResult result = performHttpCheck(ep.getUrl());

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
        } else if (wasUp && result.isUp()) {
            handleRecoveryAlert(ep);
        }
    }

    // ─────────────────────────────────────────────────────
    //  HTTP check — returns CheckResult with timing info
    //  Uses WebClient with a 10-second timeout.
    //  4xx/5xx → DOWN, exception (timeout, DNS fail) → DOWN
    // ─────────────────────────────────────────────────────
    private CheckResult performHttpCheck(String url) {
        long start = System.currentTimeMillis();
        try {
            var response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .onStatus(
                            httpStatus -> httpStatus.is4xxClientError()
                            || httpStatus.is5xxServerError(),
                            clientResponse -> clientResponse.createException()
                    )
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(10))
                    .block();

            long elapsed = System.currentTimeMillis() - start;
            int statusCode = response != null ? response.getStatusCode().value() : 0;

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
        // TODO: alertService.sendDownAlert(ep, incident);
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
                    // TODO: alertService.sendRecoveryAlert(ep, incident);
                });
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

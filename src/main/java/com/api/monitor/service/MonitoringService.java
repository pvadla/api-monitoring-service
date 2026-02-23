package com.api.monitor.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.api.monitor.entity.Endpoint;
import com.api.monitor.entity.EndpointCheck;
import com.api.monitor.repository.EndpointCheckRepository;
import com.api.monitor.repository.EndpointRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringService {

    private final EndpointRepository endpointRepository;
    private final EndpointCheckRepository endpointCheckRepository;
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
        if (ep.getLastChecked() == null) return true;
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

        // ── Incident detection (Step 7) ──────────────────
        // Alert on 2nd consecutive failure to avoid flapping alerts.
        if (wasUp && !result.isUp() && ep.getFailureCount() >= 2) {
            handleDownAlert(ep);
        } else if (!wasUp && result.isUp()) {
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
            return new CheckResult(true, elapsed, statusCode, null);

        } catch (WebClientResponseException ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("⚠️  {} → HTTP {} ({})", url, ex.getStatusCode().value(), ex.getStatusText());
            return new CheckResult(false, elapsed, ex.getStatusCode().value(), ex.getStatusText());

        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("🔴 {} → FAILED ({})", url, ex.getMessage());
            return new CheckResult(false, elapsed, null, ex.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────
    //  Incident events — Step 8 will plug email in here
    // ─────────────────────────────────────────────────────
    private void handleDownAlert(Endpoint ep) {
        log.warn("🔴 INCIDENT: '{}' ({}) is DOWN [failures: {}]",
                ep.getName(), ep.getUrl(), ep.getFailureCount());
        // TODO Step 8: emailService.sendDownAlert(ep);
    }

    private void handleRecoveryAlert(Endpoint ep) {
        log.info("🟢 RECOVERY: '{}' ({}) is back UP",
                ep.getName(), ep.getUrl());
        // TODO Step 8: emailService.sendRecoveryAlert(ep);
    }

    // ─────────────────────────────────────────────────────
    //  Simple record to carry check results around
    // ─────────────────────────────────────────────────────
    private record CheckResult(
            boolean isUp,
            long responseTimeMs,
            Integer statusCode,
            String errorMessage) {}
}
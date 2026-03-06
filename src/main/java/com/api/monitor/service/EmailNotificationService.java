package com.api.monitor.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.api.monitor.entity.Endpoint;
import com.api.monitor.entity.HeartbeatMonitor;
import com.api.monitor.entity.Incident;
import com.api.monitor.entity.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private static final String SMTP2GO_SEND_URL = "https://api.smtp2go.com/v3/email/send";

    private final WebClient webClient;

    @Value("${apiwatch.mail.enabled:false}")
    private boolean enabled;

    @Value("${apiwatch.mail.from:no-reply@apiwatch.local}")
    private String from;

    @Value("${apiwatch.app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${SMTP2GO_API_KEY:}")
    private String smtp2goApiKey;

    @Value("${apiwatch.contact.to:}")
    private String contactTo;

    public void sendEndpointDownEmail(
            User user,
            Endpoint endpoint,
            Incident incident,
            String failureReason,
            String errorMessage,
            Integer statusCode,
            long responseTimeMs) {
        if (!enabled) {
            log.debug("Email disabled; skipping down email.");
            return;
        }
        if (smtp2goApiKey == null || smtp2goApiKey.isBlank()) {
            log.warn("SMTP2GO_API_KEY not set; skipping down email.");
            return;
        }
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("No user email; skipping down email. user={}", user != null ? user.getId() : null);
            return;
        }
        if (!Boolean.TRUE.equals(user.getNotifyOnEndpointDown())) {
            log.debug("User opted out of down alerts; skipping. user={}", user.getEmail());
            return;
        }

        String subject = "APIWatch alert: " + safe(endpoint.getName()) + " is DOWN";
        String body = buildDownBody(user, endpoint, incident, failureReason, errorMessage, statusCode, responseTimeMs);

        sendEmail(user.getEmail(), subject, body);
        log.info("Down alert email sent to {} for endpoint {}", user.getEmail(), endpoint.getId());
    }

    public void sendEndpointRecoveryEmail(User user, Endpoint endpoint, Incident incident) {
        if (!enabled) {
            log.debug("Email disabled; skipping recovery email.");
            return;
        }
        if (smtp2goApiKey == null || smtp2goApiKey.isBlank()) {
            log.warn("SMTP2GO_API_KEY not set; skipping recovery email.");
            return;
        }
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("No user email; skipping recovery email. user={}", user != null ? user.getId() : null);
            return;
        }
        if (!Boolean.TRUE.equals(user.getNotifyOnEndpointRecovery())) {
            log.debug("User opted out of recovery alerts; skipping. user={}", user.getEmail());
            return;
        }

        String subject = "APIWatch: " + safe(endpoint.getName()) + " has recovered";
        String body = buildRecoveryBody(user, endpoint, incident);

        sendEmail(user.getEmail(), subject, body);
        log.info("Recovery email sent to {} for endpoint {}", user.getEmail(), endpoint.getId());
    }

    public void sendSslExpiryEmail(User user, Endpoint endpoint, LocalDateTime expiry, long daysLeft) {
        if (!enabled) {
            log.debug("Email disabled; skipping SSL expiry email.");
            return;
        }
        if (smtp2goApiKey == null || smtp2goApiKey.isBlank()) {
            log.warn("SMTP2GO_API_KEY not set; skipping SSL expiry email.");
            return;
        }
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("No user email; skipping SSL expiry email. user={}", user != null ? user.getId() : null);
            return;
        }

        String subject = "APIWatch: SSL certificate expiring soon for " + safe(endpoint.getName());
        String when = formatTime(expiry);

        String body = ""
                + "Hi " + safe(user.getName() != null ? user.getName() : "there") + ",\n\n"
                + "The SSL/TLS certificate for one of your monitored endpoints is expiring soon.\n\n"
                + "Endpoint: " + safe(endpoint.getName()) + "\n"
                + "URL: " + safe(endpoint.getUrl()) + "\n"
                + "Expires on: " + when + "\n"
                + "Time remaining: " + daysLeft + " day(s)\n\n"
                + "We recommend renewing the certificate before it expires to avoid outages and browser security warnings.\n\n"
                + "Dashboard: " + baseUrl + "/dashboard\n"
                + "Endpoint details: " + baseUrl + "/endpoints/" + endpoint.getId() + "\n\n"
                + "— APIWatch\n";

        sendEmail(user.getEmail(), subject, body);
        log.info("SSL expiry email sent to {} for endpoint {}", user.getEmail(), endpoint.getId());
    }

    public void sendHeartbeatMissedEmail(HeartbeatMonitor hb) {
        if (!enabled) {
            log.debug("Email disabled; skipping heartbeat missed email.");
            return;
        }
        if (smtp2goApiKey == null || smtp2goApiKey.isBlank()) {
            log.warn("SMTP2GO_API_KEY not set; skipping heartbeat missed email.");
            return;
        }
        User user = hb.getUser();
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("No user email; skipping heartbeat missed email. monitor={}", hb.getId());
            return;
        }

        String subject = "APIWatch: heartbeat missed for " + safe(hb.getName());
        String lastPing = hb.getLastPingAt() != null ? formatTime(hb.getLastPingAt()) : "never";
        String interval = hb.getExpectedIntervalMinutes() != null ? hb.getExpectedIntervalMinutes() + " minutes" : "unknown";

        String body = ""
                + "Hi " + safe(user.getName() != null ? user.getName() : "there") + ",\n\n"
                + "APIWatch has not received a recent heartbeat from one of your jobs.\n\n"
                + "Heartbeat name: " + safe(hb.getName()) + "\n"
                + "Expected interval: " + interval + "\n"
                + "Last ping: " + lastPing + "\n\n"
                + "If this job has failed or been disabled, please investigate. If you no longer need this monitor,\n"
                + "you can delete it from the Heartbeats page.\n\n"
                + "Heartbeats page: " + baseUrl + "/heartbeats\n\n"
                + "— APIWatch\n";

        sendEmail(user.getEmail(), subject, body);
        log.info("Heartbeat missed email sent to {} for monitor {}", user.getEmail(), hb.getId());
    }

    public void sendContactEmail(String fromName, String fromEmail, String subject, String message) {
        if (!enabled) {
            log.debug("Email disabled; skipping contact email.");
            return;
        }
        if (smtp2goApiKey == null || smtp2goApiKey.isBlank()) {
            log.warn("SMTP2GO_API_KEY not set; skipping contact email.");
            return;
        }

        String to = (contactTo != null && !contactTo.isBlank()) ? contactTo : from;
        String safeSubject = (subject == null || subject.isBlank())
                ? "New contact form message from APIWatch"
                : subject.trim();

        String body = ""
                + "You received a new message from the APIWatch contact form.\n\n"
                + "Name: " + safe(fromName) + "\n"
                + "Email: " + safe(fromEmail) + "\n\n"
                + "Message:\n"
                + safe(message) + "\n";

        sendEmail(to, safeSubject, body);
        log.info("Contact email sent from {} <{}>", fromName, fromEmail);
    }

    private void sendEmail(String toEmail, String subject, String textBody) {
        Map<String, Object> payload = Map.of(
                "sender", "APIWatch <" + from + ">",
                "to", List.of(toEmail),
                "subject", subject,
                "text_body", textBody);

        try {
            webClient.post()
                    .uri(SMTP2GO_SEND_URL)
                    .header("Content-Type", "application/json")
                    .header("X-Smtp2go-Api-Key", smtp2goApiKey)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception ex) {
            log.error("Failed to send email via SMTP2GO: {}", ex.getMessage(), ex);
            throw new RuntimeException("Email send failed: " + ex.getMessage(), ex);
        }
    }

    private String buildDownBody(
            User user,
            Endpoint endpoint,
            Incident incident,
            String failureReason,
            String errorMessage,
            Integer statusCode,
            long responseTimeMs) {
        String when = formatTime(LocalDateTime.now());
        String endpointName = safe(endpoint.getName());
        String endpointUrl = safe(endpoint.getUrl());
        String reason = safe(failureReason);
        String err = safe(errorMessage);
        String httpStatus = statusCode != null ? String.valueOf(statusCode) : "—";
        String responseTime = responseTimeMs + " ms";
        String incidentId = incident != null && incident.getId() != null ? String.valueOf(incident.getId()) : "—";

        return ""
                + "Hi " + safe(user.getName() != null ? user.getName() : "there") + ",\n\n"
                + "APIWatch detected an outage.\n\n"
                + "Endpoint: " + endpointName + "\n"
                + "URL: " + endpointUrl + "\n"
                + "Time: " + when + "\n"
                + "Status: DOWN\n"
                + "Failure reason: " + (reason.isBlank() ? "Unknown" : reason) + "\n"
                + "HTTP status: " + httpStatus + "\n"
                + "Response time: " + responseTime + "\n"
                + (err.isBlank() ? "" : ("Error details: " + err + "\n"))
                + "Incident ID: " + incidentId + "\n\n"
                + "What you can do next:\n"
                + "- Open your dashboard to review recent checks and incidents.\n"
                + "- Verify the endpoint is reachable from your environment.\n"
                + "- Check recent deployments, logs, and upstream dependencies.\n\n"
                + "Dashboard: " + baseUrl + "/dashboard\n"
                + "Endpoint details: " + baseUrl + "/endpoints/" + endpoint.getId() + "\n\n"
                + "— APIWatch\n";
    }

    private String buildRecoveryBody(User user, Endpoint endpoint, Incident incident) {
        String when = formatTime(LocalDateTime.now());
        String incidentId = incident != null && incident.getId() != null ? String.valueOf(incident.getId()) : "—";
        String downtime = incident != null && incident.getDowntimeDurationMinutes() != null
                ? (incident.getDowntimeDurationMinutes() + " minutes")
                : "—";

        return ""
                + "Hi " + safe(user.getName() != null ? user.getName() : "there") + ",\n\n"
                + "Good news — your endpoint is back up.\n\n"
                + "Endpoint: " + safe(endpoint.getName()) + "\n"
                + "URL: " + safe(endpoint.getUrl()) + "\n"
                + "Time: " + when + "\n"
                + "Status: UP\n"
                + "Incident ID: " + incidentId + "\n"
                + "Downtime: " + downtime + "\n\n"
                + "Dashboard: " + baseUrl + "/dashboard\n"
                + "Endpoint details: " + baseUrl + "/endpoints/" + endpoint.getId() + "\n\n"
                + "— APIWatch\n";
    }

    private static String formatTime(LocalDateTime t) {
        return t.format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm:ss"));
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}

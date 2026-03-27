package com.api.monitor.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.api.monitor.entity.SslMonitor;
import com.api.monitor.entity.User;
import com.api.monitor.repository.IncidentRepository;
import com.api.monitor.repository.SslCheckRepository;
import com.api.monitor.repository.SslMonitorRepository;

import lombok.RequiredArgsConstructor;

/**
 * Deletes SSL monitors and all dependent rows in a single transaction.
 * Uses bulk JPQL deletes to clear FK references before removing the monitor row,
 * avoiding foreign-key constraint violations regardless of open incidents.
 */
@Service
@RequiredArgsConstructor
public class SslMonitorDeletionService {

    private final SslMonitorRepository sslMonitorRepository;
    private final SslCheckRepository sslCheckRepository;
    private final IncidentRepository incidentRepository;

    @Transactional
    public void deleteOwnedSslMonitor(Long monitorId, User user) {
        SslMonitor monitor = sslMonitorRepository.findById(monitorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SSL monitor not found"));

        if (!monitor.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SSL monitor not found");
        }

        sslCheckRepository.deleteAllBySslMonitorId(monitorId);
        incidentRepository.deleteAllBySslMonitorId(monitorId);
        sslMonitorRepository.deleteById(monitorId);
    }
}

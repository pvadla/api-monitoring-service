package com.api.monitor.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.api.monitor.entity.HeartbeatMonitor;
import com.api.monitor.entity.User;
import com.api.monitor.repository.HeartbeatCheckRepository;
import com.api.monitor.repository.HeartbeatMonitorRepository;
import com.api.monitor.repository.IncidentRepository;

import lombok.RequiredArgsConstructor;

/**
 * Deletes heartbeat monitors and all dependent rows in a single transaction.
 * Uses bulk JPQL deletes to clear FK references before removing the monitor row,
 * avoiding foreign-key constraint violations regardless of open incidents.
 */
@Service
@RequiredArgsConstructor
public class HeartbeatDeletionService {

    private final HeartbeatMonitorRepository heartbeatMonitorRepository;
    private final HeartbeatCheckRepository heartbeatCheckRepository;
    private final IncidentRepository incidentRepository;

    @Transactional
    public void deleteOwnedHeartbeat(Long hbId, User user) {
        HeartbeatMonitor hb = heartbeatMonitorRepository.findById(hbId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Heartbeat monitor not found"));

        if (!hb.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Heartbeat monitor not found");
        }

        // Bulk-delete dependents first — this clears ALL incidents (open or resolved)
        // and all check history, preventing FK constraint violations on the monitor row.
        heartbeatCheckRepository.deleteAllByHeartbeatMonitorId(hbId);
        incidentRepository.deleteAllByHeartbeatMonitorId(hbId);
        heartbeatMonitorRepository.deleteById(hbId);
    }
}

package com.api.monitor.repository;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import com.api.monitor.entity.Endpoint;
import com.api.monitor.entity.HeartbeatMonitor;
import com.api.monitor.entity.Incident;
import com.api.monitor.entity.User;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByUserOrderByStartedAtDesc(User user, org.springframework.data.domain.Pageable pageable);

    default List<Incident> findLatestByUser(User user, int limit) {
        return findByUserOrderByStartedAtDesc(user, PageRequest.of(0, limit));
    }

    /** Delete all incidents for a user (for account deletion). */
    void deleteByUser(User user);

    /** Count of all open (unresolved) incidents for a user. */
    long countByUserAndResolvedAtIsNull(User user);

    /** Open (unresolved) auto-incident for this endpoint, if any. */
    Optional<Incident> findFirstByEndpointAndResolvedAtIsNullOrderByStartedAtDesc(Endpoint endpoint);

    /** Open (unresolved) auto-incident for this heartbeat monitor, if any. */
    Optional<Incident> findFirstByHeartbeatMonitorAndResolvedAtIsNullOrderByStartedAtDesc(HeartbeatMonitor hb);

    /** All open incidents for a heartbeat monitor (used when deleting the monitor). */
    List<Incident> findByHeartbeatMonitorAndResolvedAtIsNull(HeartbeatMonitor hb);
}

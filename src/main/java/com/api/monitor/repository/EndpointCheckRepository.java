package com.api.monitor.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.api.monitor.entity.Endpoint;
import com.api.monitor.entity.EndpointCheck;

public interface EndpointCheckRepository extends JpaRepository<EndpointCheck, Long> {

    // Last N checks for a given endpoint (for the graph)
    List<EndpointCheck> findTop50ByEndpointOrderByCheckedAtDesc(Endpoint endpoint);

    /** Newest first; used for dashboard sparkline (last 15 checks). */
    List<EndpointCheck> findTop15ByEndpointOrderByCheckedAtDesc(Endpoint endpoint);

    // Count total checks and failures (for uptime %)
    long countByEndpoint(Endpoint endpoint);
    long countByEndpointAndIsUp(Endpoint endpoint, Boolean isUp);

    // Delete all checks for a list of endpoints (for account deletion)
    void deleteByEndpointIn(List<Endpoint> endpoints);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from EndpointCheck c where c.endpoint.id = :endpointId")
    void deleteAllByEndpointId(@Param("endpointId") Long endpointId);
}
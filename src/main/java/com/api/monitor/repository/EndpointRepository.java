package com.api.monitor.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.monitor.entity.Endpoint;
import com.api.monitor.entity.User;

public interface EndpointRepository
        extends JpaRepository<Endpoint, Long> {

    // Get all endpoints for a user
    List<Endpoint> findByUser(User user);

    // Count endpoints for a user
    long countByUser(User user);

    // Count UP endpoints for a user
    long countByUserAndIsUp(User user, Boolean isUp);

    // Endpoints to show on public status page
    List<Endpoint> findByUserAndShowOnStatusPageTrue(User user);
}
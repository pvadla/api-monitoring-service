package com.api.monitor.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.monitor.entity.HeartbeatMonitor;
import com.api.monitor.entity.User;

public interface HeartbeatMonitorRepository extends JpaRepository<HeartbeatMonitor, Long> {

    List<HeartbeatMonitor> findByUser(User user);

    Optional<HeartbeatMonitor> findByToken(String token);

    List<HeartbeatMonitor> findByIsActiveTrue();
}


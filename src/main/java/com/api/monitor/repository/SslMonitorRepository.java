package com.api.monitor.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.monitor.entity.SslMonitor;
import com.api.monitor.entity.User;

public interface SslMonitorRepository extends JpaRepository<SslMonitor, Long> {

    List<SslMonitor> findByUser(User user);

    List<SslMonitor> findByIsActiveTrue();

    List<SslMonitor> findByUserAndShowOnStatusPageTrue(User user);
}

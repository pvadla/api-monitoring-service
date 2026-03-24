package com.api.monitor.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.monitor.entity.HeartbeatCheck;
import com.api.monitor.entity.HeartbeatMonitor;

public interface HeartbeatCheckRepository extends JpaRepository<HeartbeatCheck, Long> {

    /** Newest first; used for dashboard sparkline (last 15 evaluations). */
    List<HeartbeatCheck> findTop15ByHeartbeatMonitorOrderByCheckedAtDesc(HeartbeatMonitor hb);

    /** Delete all check records when a heartbeat monitor is removed. */
    void deleteByHeartbeatMonitor(HeartbeatMonitor hb);
}

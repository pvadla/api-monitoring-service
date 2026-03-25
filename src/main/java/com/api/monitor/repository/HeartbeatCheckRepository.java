package com.api.monitor.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.api.monitor.entity.HeartbeatCheck;
import com.api.monitor.entity.HeartbeatMonitor;

public interface HeartbeatCheckRepository extends JpaRepository<HeartbeatCheck, Long> {

    /** Newest first; used for dashboard sparkline (last 15 evaluations). */
    List<HeartbeatCheck> findTop15ByHeartbeatMonitorOrderByCheckedAtDesc(HeartbeatMonitor hb);

    /** Bulk delete by ID — avoids loading entities before deletion. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from HeartbeatCheck c where c.heartbeatMonitor.id = :hbId")
    void deleteAllByHeartbeatMonitorId(@Param("hbId") Long hbId);

    /** Delete all check records when a heartbeat monitor is removed. */
    void deleteByHeartbeatMonitor(HeartbeatMonitor hb);
}

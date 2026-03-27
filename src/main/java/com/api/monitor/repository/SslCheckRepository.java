package com.api.monitor.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.api.monitor.entity.SslCheck;
import com.api.monitor.entity.SslMonitor;

public interface SslCheckRepository extends JpaRepository<SslCheck, Long> {

    /** Newest-first; used for dashboard sparkline (last 15 evaluations). */
    List<SslCheck> findTop15BySslMonitorOrderByCheckedAtDesc(SslMonitor monitor);

    /** Bulk-delete by ID — used in the deletion service to clear FK references. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from SslCheck c where c.sslMonitor.id = :monitorId")
    void deleteAllBySslMonitorId(@Param("monitorId") Long monitorId);
}

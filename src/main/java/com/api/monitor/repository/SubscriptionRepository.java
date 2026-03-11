package com.api.monitor.repository;

import com.api.monitor.entity.Subscription;
import com.api.monitor.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByRazorpaySubscriptionId(String razorpaySubscriptionId);

    List<Subscription> findByUserOrderByCreatedAtDesc(User user);

    /** Active = status in (authenticated, active) and not cancelled */
    List<Subscription> findByUserAndStatusIn(User user, List<String> statuses);
}

package com.redemption.usage.domain.repository;

import com.redemption.usage.domain.model.UsageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing coupon usage history.
 * Spring Data JPA will provide the implementation at runtime.
 */
@Repository
public interface UsageHistoryRepository extends JpaRepository<UsageHistory, Long> {

    /**
     * Checks if a specific user (by IP) has already redeemed a specific coupon.
     */
    boolean existsByCouponCodeAndUserId(String couponCode, String userId);
}
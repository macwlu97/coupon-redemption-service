package com.redemption.core.domain.model;

import com.redemption.core.domain.exception.CouponException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain entity representing a Coupon.
 * Implements business logic and concurrency control via Optimistic Locking.
 */
@Entity
@Table(name = "coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String code;

    private int usageLimit;
    private int currentUsage;
    private String targetCountry;
    private LocalDateTime createdAt;

    @Version
    private Long version; // Key for scalability and thread-safety

    public Coupon(String code, int usageLimit, String targetCountry) {
        this.code = code.toUpperCase(); // Case-insensitivity requirement
        this.usageLimit = usageLimit;
        this.targetCountry = targetCountry;
        this.currentUsage = 0;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Business logic: Increments usage if rules are met. Executes the redemption logic.
     * Throws Domain Exceptions if rules are violated.
     */
    public void redeem(String userCountry) {
        validateCountry(userCountry);
        validateUsageLimit();
        this.currentUsage++;
    }

    private void validateCountry(String userCountry) {
        if (this.targetCountry != null && !this.targetCountry.equalsIgnoreCase(userCountry)) {
            throw new CouponException.InvalidCountry(userCountry, this.targetCountry);
        }
    }

    private void validateUsageLimit() {
        if (this.currentUsage >= this.usageLimit) {
            throw new CouponException.LimitExceeded(code);
        }
    }
}
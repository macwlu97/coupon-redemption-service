package com.redemption.core.domain.model;

import com.redemption.core.domain.exception.CouponException;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import java.time.LocalDateTime;
import java.util.UUID;

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

    private LocalDateTime createdAt;
    private int usageLimit;
    private int currentUsage;
    private String targetCountry;

    @Version
    private Long version; // Optimistic Locking dla wysokiej współbieżności

    public Coupon(String code, int usageLimit, String targetCountry) {
        this.code = code.toUpperCase();
        this.usageLimit = usageLimit;
        this.targetCountry = targetCountry;
        this.currentUsage = 0;
        this.createdAt = LocalDateTime.now();
    }

    public void redeem(String userCountry) {
        if (!this.targetCountry.equalsIgnoreCase(userCountry)) {
            throw new CouponException.InvalidCountry(userCountry, this.targetCountry);
        }
        if (currentUsage >= usageLimit) {
            throw new CouponException.LimitExceeded(code);
        }
        this.currentUsage++;
    }
}
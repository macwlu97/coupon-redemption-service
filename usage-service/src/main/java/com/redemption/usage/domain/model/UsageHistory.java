package com.redemption.usage.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "usage_history",
        uniqueConstraints = @UniqueConstraint(columnNames = {"couponCode", "userId"}),
        indexes = {
                @Index(name = "idx_usage_coupon_user", columnList = "couponCode, userId")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UsageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private String couponCode;

    @Column(nullable = false, updatable = false)
    private String userId; // Or userIp if we track by IP

    @Column(nullable = false, updatable = false)
    private LocalDateTime redeemedAt;

    public UsageHistory(String couponCode, String userId) {
        this.couponCode = couponCode;
        this.userId = userId;
        this.redeemedAt = LocalDateTime.now();
    }

    // 2. Automatically set timestamp before saving
    @PrePersist
    protected void onCreate() {
        this.redeemedAt = LocalDateTime.now();
    }
}
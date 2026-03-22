package com.redemption.usage.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "usage_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UsageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String couponCode;

    @Column(nullable = false)
    private String userId; // Or userIp if we track by IP

    @Column(nullable = false)
    private LocalDateTime redeemedAt;

    public UsageHistory(String couponCode, String userId) {
        this.couponCode = couponCode;
        this.userId = userId;
        this.redeemedAt = LocalDateTime.now();
    }
}
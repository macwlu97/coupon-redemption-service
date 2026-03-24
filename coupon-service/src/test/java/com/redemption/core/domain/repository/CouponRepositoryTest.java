package com.redemption.core.domain.repository;

import com.redemption.core.domain.model.Coupon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository layer test using @DataJpaTest.
 * Ensures JPA mapping and custom queries work with the actual Coupon entity logic.
 */
@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "spring.cloud.config.fail-fast=false"
})
class CouponRepositoryTest {

    @Autowired
    private CouponRepository couponRepository;

    @Test
    @DisplayName("Repository: Should find coupon by its unique code and verify case-insensitivity")
    void shouldFindCouponByCode() {
        // GIVEN: Using the actual constructor from your Coupon entity
        String inputCode = "winter-sale"; // lowercase input
        Coupon coupon = new Coupon(inputCode, 100, "PL");
        couponRepository.save(coupon);

        // WHEN: Retrieve the coupon using the uppercase version (as logic dictates)
        Optional<Coupon> found = couponRepository.findByCode("WINTER-SALE");

        // THEN
        assertThat(found).isPresent();
        assertThat(found.get().getCode()).isEqualTo("WINTER-SALE");
        assertThat(found.get().getTargetCountry()).isEqualTo("PL");
    }

    @Test
    @DisplayName("Repository: Should return empty Optional when code does not exist")
    void shouldReturnEmptyWhenCodeNotFound() {
        // GIVEN
        String nonExistentCode = "GHOST-CODE";

        // WHEN
        Optional<Coupon> found = couponRepository.findByCode(nonExistentCode);

        // THEN
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Repository: Should persist all coupon fields and generate UUID")
    void shouldPersistCouponFields() {
        // GIVEN: Testing the standard flow
        Coupon coupon = new Coupon("SAVE50", 50, "DE");

        // WHEN
        Coupon saved = couponRepository.saveAndFlush(coupon);

        // THEN: Verify UUID generation and initial values
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUsageLimit()).isEqualTo(50);
        assertThat(saved.getCurrentUsage()).isZero();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getVersion()).isNotNull(); // Optimistic locking version
    }
}
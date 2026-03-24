package com.redemption.usage.domain.repository;

import com.redemption.usage.domain.model.UsageHistory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository test using @DataJpaTest to verify database interactions.
 * This starts a lightweight Spring context with an in-memory database (H2).
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=optional:configserver:",
        "spring.cloud.config.fail-fast=false"
})
@ActiveProfiles("test")
class UsageHistoryRepositoryTest {

    @Autowired
    private UsageHistoryRepository repository;

    @Test
    @DisplayName("Repository: Should return true when usage history already exists")
    void shouldReturnTrueWhenRecordExists() {
        // GIVEN: Save a record to the database
        String code = "PROMO2026";
        String userId = "192.168.1.1";
        repository.save(new UsageHistory(code, userId));

        // WHEN: Check for existence
        boolean exists = repository.existsByCouponCodeAndUserId(code, userId);

        // THEN: It should be found
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Repository: Should return false when record does not exist")
    void shouldReturnFalseWhenRecordDoesNotExist() {
        // GIVEN: Database is empty (or has different records)
        String code = "NEW-CODE";
        String userId = "8.8.8.8";

        // WHEN: Check for existence
        boolean exists = repository.existsByCouponCodeAndUserId(code, userId);

        // THEN: It should not be found
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Repository: Should return false when only one parameter matches")
    void shouldReturnFalseWhenOnlyPartialMatch() {
        // GIVEN: Save a record
        repository.save(new UsageHistory("CODE-A", "USER-1"));

        // WHEN: Search with matching code but different user
        boolean exists = repository.existsByCouponCodeAndUserId("CODE-A", "USER-2");

        // THEN: It should be false because of the AND condition in method name
        assertThat(exists).isFalse();
    }
}